package HyDCV3;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;

import java.util.*;


public class MMCSNode {

    public int numberOfPredicates;
    /**
     *  the set of predicates which can cover the evidence set
    */
    private IBitSet element;

    IBitSet candidatePredicates;

    public HashEvidenceSet uncoverEvidenceSet;

    public List<List<PredicateBitSet>> crit;

    public IBitSet getElement() {
        return element;
    }

    public List<ClusterPair> clusterPairs;
    //TODO: we can remain dc in every node, may be can accelerate process
    public PredicateBitSet denialConstraint = new PredicateBitSet();

    public MMCSNode(int numberOfPredicates, IEvidenceSet evidenceToCover) {

        this.numberOfPredicates = numberOfPredicates;

        element = new LongBitSet();

        uncoverEvidenceSet = (HashEvidenceSet) evidenceToCover;

        if (MMCSDC.candidatePredicates != null) candidatePredicates = MMCSDC.candidatePredicates;
        else {
            candidatePredicates = new LongBitSet();
            for (int i = 0; i < numberOfPredicates; ++i) {
                candidatePredicates.set(i);
            }
        }

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

        // update denialConstraint in time
        Predicate inverse = PredicateBitSet.indexProvider.getObject(predicateAdded).getInverse();
        denialConstraint.getBitset().set(PredicateBitSet.indexProvider.getIndex(inverse), true);

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

    public void clearCrit(){
        for (List<PredicateBitSet> c : crit)
            c.clear();
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
