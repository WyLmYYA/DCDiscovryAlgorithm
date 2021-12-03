package HyDC;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import HyDC.MMCSDC;

import java.util.*;

/**
 * @Author yoyuan
 * @Description:   tree node in the deep transversal of MMCS for DC
 * @DateTime: 2021/9/26 14:56
 */
public class MMCSNode {

    private int numberOfPredicates;
    /**
     *  the set of predicates which can cover the evidence set
    */
    private IBitSet element;

    IBitSet candidatePredicates;

    public HashEvidenceSet uncoverEvidenceSet;

    private List<List<PredicateBitSet>> crit;

    public IBitSet getElement() {
        return element;
    }

    /**
     * @Author yongan.yuan
     * @Description: add attributes for HyDC
     */

    // if predicate of node is not equal, we need to combine another to run BITJoin
    public boolean isNeedCombine = false;

    public ClusterPair clusterPair;

    // new added evidenceSet from childNode, initial is null
    public HashEvidenceSet newEvidenceSet = null;


    public MMCSNode(int numberOfPredicates, HashEvidenceSet evidenceToCover) {

        this.numberOfPredicates = numberOfPredicates;

        element = new LongBitSet();

        uncoverEvidenceSet = evidenceToCover;

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
        return uncoverEvidenceSet.isEmpty() && clusterPair.isEmpty();
    }


    public PredicateBitSet getNextEvidence() {

        Comparator<PredicateBitSet> cmp = Comparator.comparing(predicates -> predicates.getBitset().getAnd(candidatePredicates));

        return Collections.min(uncoverEvidenceSet.getSetOfPredicateSets(), cmp);

    }

    public MMCSNode getChildNode(int predicateAdded, IBitSet nextCandidatePredicates) {

        MMCSNode childNode = new MMCSNode(numberOfPredicates);

        //TODO: get new Cluster Pair from parent node by BITJoin

        childNode.refine(this);

        childNode.cloneContext(nextCandidatePredicates, this);

        childNode.updateContextFromParent(predicateAdded, this);

        return childNode;

    }

    private void refine(MMCSNode parentNode){
        if (parentNode.isNeedCombine && this.isNeedCombine){
            // TODO: refine use BITJoin
        }else if (!parentNode.isNeedCombine && this.isNeedCombine){
            // TODO：wait next combine
            //  if this node is the last one need combine, how to solve?
        }else{
            // TODO: use normal way
        }
    }
    private void updateContextFromParent(int predicateAdded, MMCSNode node) {

        //TODO: we can change TroveEvidenceSet to see which one is more suitable
        uncoverEvidenceSet = new HashEvidenceSet();

        /**
         *  after add new Predicates, some uncoverEvidenceSet can be removed, so we don't need add them to childNode
         *  and we should update crit meanwhile
        */
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

    }

    private void cloneContext(IBitSet nextCandidatePredicates, MMCSNode parentNode) {
        element = parentNode.element.clone();

        candidatePredicates = nextCandidatePredicates.clone();

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
