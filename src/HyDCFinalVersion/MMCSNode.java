package HyDCFinalVersion;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.PartitionEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.IEJoin;
import Hydra.de.hpi.naumann.dc.paritions.LinePair;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.PredicatePair;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;

import java.util.*;

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

    public MMCSNode(int numberOfPredicates, IEvidenceSet evidenceToCover) {

        this.numberOfPredicates = numberOfPredicates;

        element = new LongBitSet();

        uncoverEvidenceSet = (HashEvidenceSet) evidenceToCover;

        candidatePredicates = MMCSDC.candidatePredicates;

        crit = new ArrayList<>(numberOfPredicates);
        for (int i = 0; i < numberOfPredicates; ++i){
            crit.add(new ArrayList<>());
        }
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

        // in clone step, curNode get cluster pair from parent
        if (predicate.needCombine()){
            // 1. if this one is not first predicate need combination, use IEJoin update cluster pair
            if (node.lastNeedCombinationPredicate != null){
                refinePP(predicate, node.lastNeedCombinationPredicate);
            }else {
            // 2. if this node is the first predicate need combination, get cluster pair from parent, wait for next refine
                lastNeedCombinationPredicate = predicate;
            }
        }else {
            // if this node is not needed combination
            refinePS(predicate, MMCSDC.ieJoin);
            // 3. if parent or parent before has node wait for refining
            if (node.lastNeedCombinationPredicate != null){
                lastNeedCombinationPredicate = node.lastNeedCombinationPredicate;
            }
        }

    }
    public void refinePS(Predicate predicate, IEJoin ieJoin){
        // refine by single predicate
        List<ClusterPair> newResult = new ArrayList<>();
        clusterPairs.forEach(clusterPair -> {
            clusterPair.refinePsPublic(predicate, ieJoin, newResult);
        });
        clusterPairs = newResult;
    }
    public void refinePP(Predicate p1,  Predicate p2){
        // refine by Join

        List<ClusterPair> newResult = new ArrayList<>();
        clusterPairs.forEach(clusterPair -> {
            clusterPair.refinePPPublic(new PredicatePair(p1, p2), MMCSDC.ieJoin, pair -> newResult.add(pair));
        });
        clusterPairs = newResult;
    }

    private void cloneContext(IBitSet nextCandidatePredicates, MMCSNode parentNode) {
        element = parentNode.element.clone();

        candidatePredicates = nextCandidatePredicates.clone();

        clusterPairs = new ArrayList<>(parentNode.clusterPairs);

        crit = new ArrayList<>(numberOfPredicates);
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

    /**
     * valid current node
     */
    public void valid(){
//        if ()
    }

    public void getAddedEvidenceSet(PredicateBuilder predicates, Input input){
        clusterPairs.forEach(clusterPair -> {
            new PartitionEvidenceSetBuilder(predicates, input.getInts()).addEvidences(clusterPair, addEvidences);
        });
        uncoverEvidenceSet.add(addEvidences);
        clusterPairs = null;
    }
    public boolean isValidResult(){
        // there may be {12} : {12, 12}, so need to add one step to judge
        if (clusterPairs == null)return true;
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
