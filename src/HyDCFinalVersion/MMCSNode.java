package HyDCFinalVersion;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.ch.javasoft.bitset.search.NTreeSearch;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.PartitionEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.paritions.*;
import Hydra.de.hpi.naumann.dc.predicates.Operator;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.PredicatePair;
import Hydra.de.hpi.naumann.dc.predicates.sets.Closure;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateSetFactory;
import utils.TimeCal;

import java.util.*;

import static Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet.indexProvider;

public class MMCSNode {

    private int numberOfPredicates;
    /**
     *  the set of predicates which can cover the evidence set
    */
    public IBitSet element;

    IBitSet candidatePredicates;

    public HashEvidenceSet uncoverEvidenceSet;

    public List<List<PredicateBitSet>> crit;

    public IBitSet getElement() {
        return element;
    }

    /**
     * params added for hyDC
     */
    public boolean isCombination = false, isFirstNeededCombination = false;

    public Predicate lastNeedCombinationPredicate;


    public List<ClusterPair> clusterPairs = new ArrayList<>();

    public HashEvidenceSet addEvidences = new HashEvidenceSet();

    public List<Integer> sortedPredicates = new ArrayList<>();

    public List<Predicate> sortedPredicates1 = new ArrayList<>();

    public List<MMCSNode> nodesInPath = new ArrayList<>();

    public Predicate curPred;

    public MMCSNode(int numberOfPredicates, IEvidenceSet evidenceToCover, int lineCount) {

        this.numberOfPredicates = numberOfPredicates;

        element = new LongBitSet();

        uncoverEvidenceSet = (HashEvidenceSet) evidenceToCover;

        candidatePredicates = MMCSDC.candidatePredicates;

        clusterPairs.add(StrippedPartition.getFullParition(lineCount));

        crit = new ArrayList<>(numberOfPredicates);
        for (int i = 0; i < numberOfPredicates; ++i){
            crit.add(new ArrayList<>());
        }
        nodesInPath.add(this);

    }

    public MMCSNode(int numberOfPredicates) {

        this.numberOfPredicates = numberOfPredicates;

    }

    public boolean canCover() {
        return uncoverEvidenceSet.isEmpty();
    }

    public void resetCandidatePredicates(IBitSet mask){
        candidatePredicates = mask.getAndNot(element);
    }

    public PredicateBitSet getNextEvidence() {

        Comparator<PredicateBitSet> cmp = Comparator.comparing(predicates -> predicates.getBitset().getAnd(candidatePredicates));

        return Collections.min(uncoverEvidenceSet.getSetOfPredicateSets(), cmp);

    }
    public MMCSNode(MMCSNode mmcsNode) {

        this.numberOfPredicates = mmcsNode.numberOfPredicates;

        element = mmcsNode.getElement().clone();

        uncoverEvidenceSet.add(mmcsNode.uncoverEvidenceSet);

        candidatePredicates = mmcsNode.candidatePredicates.clone();

        clusterPairs = mmcsNode.clusterPairs;

        // TODO: crit的问题
        crit = new ArrayList<>(mmcsNode.crit.size());
        for (List<PredicateBitSet> predicateBitSets : mmcsNode.crit) {
            crit.add(new ArrayList<>(predicateBitSets));
        }

    }

    public MMCSNode getChildNode(int predicateAdded, IBitSet nextCandidatePredicates) {

        MMCSNode childNode = new MMCSNode(numberOfPredicates);

        childNode.cloneContext(nextCandidatePredicates, this);

        childNode.updateContextFromParent(predicateAdded, this);

        return childNode;

    }

    private void updateContextFromParent(int predicateAdded, MMCSNode node) {

        //TODO: we can change TroveEvidenceSet to see which one is more suitable
        uncoverEvidenceSet = new HashEvidenceSet();

        /**
         *  after add new Predicates, some uncoverEvidenceSet can be removed, so we don't need add them to childNode
         *  and we should update crit meanwhile
        */

        // parent uncover evidenceSet has updated before,
        // in other words parent uncover has added newEvidence come from before nodes.
        // so we can update directly
        node.uncoverEvidenceSet.getSetOfPredicateSets().forEach(predicates -> {
            if(predicates.getBitset().get(predicateAdded)) crit.get(predicateAdded).add(predicates);
            else uncoverEvidenceSet.add(predicates);
        });

        /**
         *  update crit in terms of  the EvidenceSet which is covered by predicateAdded
        */
        for (int next = element.nextSetBit(0); next >= 0; next = element.nextSetBit(next + 1))
            crit.get(next).removeIf(predicates -> predicates.getBitset().get(predicateAdded));

        /**
         *  update element
        */
        element.set(predicateAdded, true);

        /**
         *  judge current node predicate, and update clusterPairs
         */
        Predicate predicate = PredicateBitSet.indexProvider.getObject(predicateAdded);

        curPred = predicate;

        sortedPredicates.add(predicateAdded);

        sortedPredicates1.add(predicate);

        nodesInPath.add(this);


//        // in clone step, curNode get cluster pair from parent
//        if (predicate.needCombine()){
//            // 1. if this one is not first predicate need combination, use IEJoin update cluster pair
//            if (node.lastNeedCombinationPredicate != null){
//                refinePP(predicate, node.lastNeedCombinationPredicate);
//            }else {
//                // 2. if this node is the first predicate need combination, get cluster pair from parent, wait for next refine
//                lastNeedCombinationPredicate = predicate;
//            }
//        }else {
//            // if this node is not needed combination
//            refinePS(predicate, MMCSDC.ieJoin);
//            // 3. if parent or parent before has node wait for refining
//            if (node.lastNeedCombinationPredicate != null){
//                lastNeedCombinationPredicate = node.lastNeedCombinationPredicate;
//            }
//        }



    }
    public void refinePS(Predicate predicate, IEJoin ieJoin){
        long l1 = System.currentTimeMillis();
        // refine by single predicate
        List<ClusterPair> newResult = new ArrayList<>();
        clusterPairs.forEach(clusterPair -> {
            clusterPair.refinePsPublic(predicate.getInverse(), ieJoin, newResult);
        });
        clusterPairs = newResult;
        TimeCal.add((System.currentTimeMillis() - l1), 0);
    }
    public void refinePP(Predicate p1,  Predicate p2){
        // refine by Join
        long l1 = System.currentTimeMillis();
        List<ClusterPair> newResult = new ArrayList<>();
        clusterPairs.forEach(clusterPair -> {
            clusterPair.refinePPPublic(new PredicatePair(p1.getInverse(), p2.getInverse()), MMCSDC.ieJoin, pair -> newResult.add(pair));
        });
        clusterPairs = newResult;
        TimeCal.add((System.currentTimeMillis() - l1), 0);
    }

    public void refine(){
        int firstRefined = 0;
        for(int i = 0; i < nodesInPath.size(); ++i){
            if (nodesInPath.get(i).clusterPairs.size() != 0){
                firstRefined = i;
                break;
            }
        }
        while (firstRefined < nodesInPath.size() - 1){
            MMCSNode node = nodesInPath.get(firstRefined);
            MMCSNode curNode = nodesInPath.get(firstRefined + 1);
            curNode.clusterPairs = new ArrayList<>(node.clusterPairs);
            Predicate currentPredicate = curNode.curPred;
            if (currentPredicate.needCombine()){
                // 1. if this one is not first predicate need combination, use IEJoin update cluster pair
                if (node.lastNeedCombinationPredicate != null){
                    curNode.refinePP(currentPredicate, node.lastNeedCombinationPredicate);
                }else {
                    // 2. if this node is the first predicate need combination, get cluster pair from parent, wait for next refine
                    curNode.lastNeedCombinationPredicate = currentPredicate;
                }
            }else {
                // if this node is not needed combination
                curNode.refinePS(currentPredicate, MMCSDC.ieJoin);
                // 3. if parent or parent before has node wait for refining
                if (node.lastNeedCombinationPredicate != null){
                    curNode.lastNeedCombinationPredicate = node.lastNeedCombinationPredicate;
                }
            }
            firstRefined++;
        }


    }

    private void cloneContext(IBitSet nextCandidatePredicates, MMCSNode parentNode) {
        element = parentNode.element.clone();

        candidatePredicates = nextCandidatePredicates.clone();

//        clusterPairs = new ArrayList<>(parentNode.clusterPairs);


        crit = new ArrayList<>(numberOfPredicates);

        sortedPredicates = new ArrayList<>(parentNode.sortedPredicates);

        sortedPredicates1 = new ArrayList<>(parentNode.sortedPredicates1);

//        neededValidCount = parentNode.neededValidCount;

        parentNode.nodesInPath.forEach(mmcsNode -> nodesInPath.add(mmcsNode));

        for (int i = 0; i < numberOfPredicates; ++i){
            crit.add(new ArrayList<>(parentNode.crit.get(i)));
        }

    }

    /**
     *  Judge current node is global minimal or not by crit
    */
    public boolean isGlobalMinimal() {
        for(int next = element.nextSetBit(0); next >= 0; next = element.nextSetBit(next+1))
            if(crit.get(next).isEmpty()) return false;
        return true;
    }


    public void getAddedEvidenceSet(){
        HashEvidenceSet newEvi = new HashEvidenceSet();
        clusterPairs.forEach(clusterPair -> {
            new PartitionEvidenceSetBuilder(MMCSDC.predicates, MMCSDC.input.getInts()).addEvidences(clusterPair, newEvi);
        });
        Iterator iterable = newEvi.getSetOfPredicateSets().iterator();
        while (iterable.hasNext()){
            PredicateBitSet predicates1 = (PredicateBitSet) iterable.next();
            if (predicates1.getBitset().getAnd(element).cardinality() != 0){
                iterable.remove();
            }

        }
        uncoverEvidenceSet.add(newEvi);
        addEvidences.add(newEvi);
        clusterPairs = new ArrayList<>();
    }
    public boolean isValidResult(){
        // there may be {12} : {12, 12}, so need to add one step to judge
        for (ClusterPair clusterPair : clusterPairs){
            Iterator<LinePair> iter = clusterPair.getLinePairIterator();
            while (iter.hasNext()) {
                LinePair lPair = iter.next();
                int i = lPair.getLine1();
                int j = lPair.getLine2();
                if (i != j) return false;
            }
        }
        return true;


    }

    public boolean isPrunedByTransivity(){
        Predicate predicate = indexProvider.getObject(sortedPredicates.get(sortedPredicates.size() - 1));
        Operator[] trans = predicate.getOperator().getTransitives();
        for (int ne = element.nextSetBit(0); ne != -1; ne = element.nextSetBit(ne + 1)){
            Predicate predicate2 = indexProvider.getObject(ne);
            if (predicate == predicate2) continue;
            for (Operator op : trans){
                if (op.equals(predicate2.getOperator()) && predicate.getOperand2().getColumn().equals(predicate2.getOperand1().getColumn())){
                    Predicate newPred = new Predicate(op, predicate.getOperand1(), predicate2.getOperand2());
                    if (canBeReplaced(sortedPredicates.get(sortedPredicates.size() - 1), ne, newPred)) return true;
                }
                if (op.equals(predicate2.getOperator()) && predicate.getOperand1().getColumn().equals(predicate2.getOperand2().getColumn())){
                    Predicate newPred = new Predicate(op, predicate2.getOperand1(), predicate.getOperand2());
                    if (canBeReplaced(sortedPredicates.get(sortedPredicates.size() - 1), ne, newPred)) return true;
                }
            }
        }
        return false;
    }

    public boolean canBeReplaced(int p1, int p2, Predicate newPre){
        for (PredicateBitSet predicates : crit.get(p1)) {
            if (!predicates.getBitset().get(indexProvider.getIndex(newPre))){
                return false;
            }
        }
        for (PredicateBitSet predicates : crit.get(p2) ) {
            if (!predicates.getBitset().get(indexProvider.getIndex(newPre))){
                return false;
            }
        }
        return true;
    }


    public DenialConstraint getDenialConstraint() {
        PredicateBitSet inverse = new PredicateBitSet();
        for (int next = element.nextSetBit(0); next >= 0; next = element.nextSetBit(next + 1)) {
            Predicate predicate = indexProvider.getObject(next); //1
            inverse.add(predicate.getInverse());
        }
        return new DenialConstraint(inverse);
    }


    public boolean isTransivityValid(NTreeSearch nTreeSearch, boolean[] v, boolean hasInverse){
        if (nTreeSearch.bitset != null && nTreeSearch.subtrees.size() == 0) return false;

        for (int i = 0; i < sortedPredicates.size(); ++i){
            int predicate = sortedPredicates.get(i);
            if (v[i])continue;
            v[i] = true;
            if (nTreeSearch.subtrees.containsKey(predicate) && !isTransivityValid(nTreeSearch.subtrees.get(predicate), v, hasInverse)){
                return false;
            }
            if (hasInverse) continue;
            Predicate inverse = indexProvider.getObject(predicate).getInverse();

            for (Integer p : nTreeSearch.subtrees.keySet()){
                List<Predicate> impl = (List<Predicate>) indexProvider.getObject(p).getImplications();
                if (impl.contains(inverse) && !isTransivityValid(nTreeSearch.subtrees.get(p), v, true)){
                    return false;
                }
            }
            v[i] = false;

        }
        return true;

    }

//    public boolean minimize(Map<PredicateBitSet, DenialConstraintSet.MinimalDCCandidate> constraintsClosureMap, NTreeSearch tree){
//        PredicateBitSet predicates = new PredicateBitSet(element);
//
//        Closure c = new Closure(predicates);
//        if (c.construct()) {
//            DenialConstraintSet.MinimalDCCandidate candidate = new DenialConstraintSet.MinimalDCCandidate(getDenialConstraint());
//            PredicateBitSet closure = c.getClosure();
//            DenialConstraintSet.MinimalDCCandidate prior = constraintsClosureMap.get(closure);
//            if (candidate.shouldReplace(prior)) {
//                constraintsClosureMap.put(closure, candidate);
//
//                // search if therr DC.pres ∈ closure， if so, this node is implied
//                if (tree.containsSubset(closure.getBitset()))
//                    return false;
//                DenialConstraint inv = candidate.dc.getInvT1T2DC();
//                if (inv != null) {
//                    Closure closure1 = new Closure(inv.getPredicateSet());
//                    if (!closure1.construct() || tree.containsSubset(PredicateSetFactory.create(closure1.getClosure()).getBitset()))
//                        return false;
//                }
//                tree.add(candidate.bitset);
//                if(inv != null)
//                    tree.add(PredicateSetFactory.create(inv.getPredicateSet()).getBitset());
//                return true;
//            }
//
//        }
//        return false;
//
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MMCSNode mmcsNode = (MMCSNode) o;
        return numberOfPredicates == mmcsNode.numberOfPredicates && Objects.equals(element, mmcsNode.element) && Objects.equals(candidatePredicates, mmcsNode.candidatePredicates) && Objects.equals(uncoverEvidenceSet, mmcsNode.uncoverEvidenceSet) && Objects.equals(crit, mmcsNode.crit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfPredicates, element, candidatePredicates, uncoverEvidenceSet, crit);
    }
}
