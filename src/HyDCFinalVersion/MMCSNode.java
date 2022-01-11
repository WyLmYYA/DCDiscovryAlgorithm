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
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import utils.TimeCal;
import utils.TimeCal2;
import utils.TimeCal3;

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

    public List<MMCSNode> nodesInPath = new ArrayList<>();

    public Predicate curPred;

    public boolean needRefine = true;

    public static PartitionEvidenceSetBuilder partitionEvidenceSetBuilder;

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

    public static long time = 0;
    public PredicateBitSet getNextEvidence() {
//        long l1 = System.currentTimeMillis();
//        for (PredicateBitSet predicates : uncoverEvidenceSet) {
//            if (predicates.allCover == 0){
//                predicates.forEach(predicate -> predicates.allCover += predicate.coverSize);
//            }
//        }
//        time += System.currentTimeMillis() - l1;
//        Comparator<PredicateBitSet> cmp = Comparator.comparing(predicates -> predicates.allCover);
//
//        return Collections.max(uncoverEvidenceSet.getSetOfPredicateSets(), cmp);

        Comparator<PredicateBitSet> cmp = Comparator.comparing(predicates -> predicates.getBitset().getAnd(candidatePredicates));

        return Collections.min(uncoverEvidenceSet.getSetOfPredicateSets(), cmp);

    }
    public MMCSNode(MMCSNode mmcsNode) {

        this.numberOfPredicates = mmcsNode.numberOfPredicates;

        element = mmcsNode.getElement().clone();

        uncoverEvidenceSet.add(mmcsNode.uncoverEvidenceSet);

        candidatePredicates = mmcsNode.candidatePredicates.clone();

        clusterPairs = mmcsNode.clusterPairs;

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

        nodesInPath.add(this);



    }
    public void refinePS(Predicate predicate, IEJoin ieJoin){

        long l1 = System.currentTimeMillis();
        // refine by single predicate
        List<ClusterPair> newResult = new ArrayList<>();
        clusterPairs.forEach(clusterPair -> {
            TimeCal2.add(1,4);
            clusterPair.refinePsPublic(predicate.getInverse(), ieJoin, newResult);
        });
        clusterPairs = newResult;
        TimeCal3.addPreCal(predicate, 1);
        TimeCal3.add(predicate, System.currentTimeMillis() - l1);
        TimeCal2.add((System.currentTimeMillis() - l1), 0);
    }
    public void refinePP(Predicate p1,  Predicate p2){
        // refine by Join

        long l1 = System.currentTimeMillis();
        List<ClusterPair> newResult = new ArrayList<>();
        clusterPairs.forEach(clusterPair -> {
            TimeCal2.add(1,5);
            clusterPair.refinePPPublic(new PredicatePair(p1.getInverse(), p2.getInverse()), MMCSDC.ieJoin, clusterPair1 -> newResult.add(clusterPair1));
        });
        clusterPairs = newResult;
        TimeCal3.addPreCal(p1, 1);
        TimeCal3.addPreCal(p2, 1);
        TimeCal3.add(p1, System.currentTimeMillis() - l1);
        TimeCal3.add(p2, System.currentTimeMillis() - l1);
        TimeCal2.add((System.currentTimeMillis() - l1), 0);
    }

    public void refineAlways(){
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

    public void refine(){
        int firstRefined = 0;
        List<Predicate> needCombination = new ArrayList<>();
        for(int i = 0; i < nodesInPath.size(); ++i){
            if (nodesInPath.get(i).curPred != null && nodesInPath.get(i).curPred.needCombine()){
                needCombination.add(nodesInPath.get(i).curPred);
            }
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
                    needCombination.add(currentPredicate);
            }else {
                // if this node is not needed combination
                curNode.refinePS(currentPredicate, MMCSDC.ieJoin);
            }
//            if (firstRefined == nodesInPath.size() - 2)
//                curNode.refineCombinationEnd(needCombination);
            firstRefined++;
        }


    }

    public static long pairTime = 0;
    private void refineCombinationEnd(List<Predicate> predicates){
        long l2 = System.currentTimeMillis();
        Multiset<PredicatePair> paircountDC = HashMultiset.create();
        predicates.forEach(p1 -> {
            if (StrippedPartition.isPairSupported(p1)){
                predicates.forEach(p2 -> {
                    if (!p1.equals(p2) && StrippedPartition.isPairSupported(p2)) {
                        paircountDC.add(new PredicatePair(p1, p2));
                    }
                });
            }
        });
        pairTime += System.currentTimeMillis() - l2;
        for (PredicatePair predicatePair : paircountDC){
            List<ClusterPair> newResult = new ArrayList<>();
            Predicate p1 = predicatePair.getP1();
            Predicate p2 = predicatePair.getP2();
            long l1 = System.currentTimeMillis();
            clusterPairs.forEach(clusterPair -> {
                TimeCal2.add(1,5);
                clusterPair.refinePPPublic(new PredicatePair(p1.getInverse(), p2.getInverse()), MMCSDC.ieJoin, clusterPair1 -> newResult.add(clusterPair1));
            });
            clusterPairs = newResult;
            TimeCal3.addPreCal(p1, 1);
            TimeCal3.addPreCal(p2, 1);
            TimeCal3.add(p1, System.currentTimeMillis() - l1);
            TimeCal3.add(p2, System.currentTimeMillis() - l1);
            TimeCal2.add((System.currentTimeMillis() - l1), 0);
        }
    }

    private void cloneContext(IBitSet nextCandidatePredicates, MMCSNode parentNode) {
        element = parentNode.element.clone();

        candidatePredicates = nextCandidatePredicates.clone();

//        clusterPairs = new ArrayList<>(parentNode.clusterPairs);


        crit = new ArrayList<>(numberOfPredicates);


//        neededValidCount = parentNode.neededValidCount;

         needRefine = parentNode.needRefine;

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


        long l2 = System.currentTimeMillis();

        if (partitionEvidenceSetBuilder == null){
            partitionEvidenceSetBuilder = new PartitionEvidenceSetBuilder(MMCSDC.predicates, MMCSDC.input.getInts());
        }
        for (ClusterPair clusterPair : clusterPairs){
            partitionEvidenceSetBuilder.addEvidences(clusterPair, newEvi);
        }
        TimeCal2.add((System.currentTimeMillis() - l2), 2);


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
        needRefine = false;
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



    public DenialConstraint getDenialConstraint() {
        PredicateBitSet inverse = new PredicateBitSet();
        for (int next = element.nextSetBit(0); next >= 0; next = element.nextSetBit(next + 1)) {
            Predicate predicate = indexProvider.getObject(next); //1
            inverse.add(predicate.getInverse());

        }
        return new DenialConstraint(inverse);
    }

    public DenialConstraint getDenialConstraint(int addPredicate) {
        PredicateBitSet inverse = new PredicateBitSet();
        for (int next = element.nextSetBit(0); next >= 0; next = element.nextSetBit(next + 1)) {
            Predicate predicate = indexProvider.getObject(next); //1
            inverse.add(predicate.getInverse());
        }
        inverse.add(indexProvider.getObject(addPredicate));
        return new DenialConstraint(inverse);
    }

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
