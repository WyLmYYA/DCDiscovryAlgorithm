package HyDC;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.IEJoin;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import gnu.trove.list.array.TIntArrayList;
import utils.TimeCal;

import java.util.*;

/**
 * @Author yoyuan
 * @Description:   tree node in the deep transversal of MMCS for DC
 * @DateTime: 2021/9/26 14:56
 */
public class MMCSHyDCNode {

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

    public List<Predicate> predicatesForElement = new ArrayList<>();

    // new added evidenceSet from childNode, initial is null
    public HashEvidenceSet newEvidenceSet = null;

    // maintain evidence set from beg to end
    public HashEvidenceSet completeEvidenceSet;


    public MMCSHyDCNode( int lineCount, int numberOfPredicates, HashEvidenceSet evidenceToCover ) {

        this.numberOfPredicates = numberOfPredicates;

        element = new LongBitSet();

        uncoverEvidenceSet = evidenceToCover;

        completeEvidenceSet = evidenceToCover;

        //Initial ClusterPair only one full lineCount
        TIntArrayList c = new TIntArrayList();
        for(int i = 0; i < lineCount; ++i){
            c.add(i);
        }

        candidatePredicates = MMCSHyDC.candidatePredicates;

        newEvidenceSet = new HashEvidenceSet();
        crit = new ArrayList<>(numberOfPredicates);
        for (int i = 0; i < numberOfPredicates; ++i){
            crit.add(new ArrayList<>());
        }
    }

    public MMCSHyDCNode(int numberOfPredicates) {

        this.numberOfPredicates = numberOfPredicates;

        newEvidenceSet = new HashEvidenceSet();
    }

    public boolean canCover() {
        return uncoverEvidenceSet.isEmpty();
    }


    public PredicateBitSet getNextEvidence() {

        Comparator<PredicateBitSet> cmp = Comparator.comparing(predicates -> predicates.getBitset().getAnd(candidatePredicates));

        return Collections.min(uncoverEvidenceSet.getSetOfPredicateSets(), cmp);

    }

    public MMCSHyDCNode getChildNode(int predicateAdded, IBitSet nextCandidatePredicates, IEJoin ieJoin) {

        MMCSHyDCNode childNode = new MMCSHyDCNode(numberOfPredicates);

        childNode.cloneContext(nextCandidatePredicates, this);

        boolean isGlobalMini = childNode.updateContextFromParent(predicateAdded, this);

        if (!isGlobalMini) return null;

        return childNode;

    }


    private boolean updateContextFromParent(int predicateAdded, MMCSHyDCNode node) {

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

        if (!this.isGlobalMinimal()){
            return false;
        }

        /**
         *  update element
        */
        element.set(predicateAdded, true);

        predicatesForElement.add(PredicateBitSet.indexProvider.getObject(predicateAdded));

        return true;

    }

    private void cloneContext(IBitSet nextCandidatePredicates, MMCSHyDCNode parentNode) {


        /** clone don't time */
        element = parentNode.element.clone();

        candidatePredicates = nextCandidatePredicates.clone();

        completeEvidenceSet = parentNode.completeEvidenceSet;



        // TODO: can combine with update
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

    public void refineClusterPairs(List<ClusterPair> result){
        Map<Integer, Set<Integer>> map = new HashMap<>();
        result.removeIf(clusterPair -> {
            Cluster c1 = clusterPair.getC1();
            Cluster c2 = clusterPair.getC2();
            for (int c1Val : c1.toArray()){
                if (!map.containsKey(c1Val)){
                    map.put(c1Val, c2.toSet());
                    continue;
                }
                Set<Integer> tmp = map.get(c1Val);
                for (int c2Val : c2.toArray()){
                    if (tmp.add(c2Val)){
                        c2.remove(c2Val);
                    }
                }
            }
            return !clusterPair.containsLinePair();
        });
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MMCSHyDCNode mmcsNode = (MMCSHyDCNode) o;
        return numberOfPredicates == mmcsNode.numberOfPredicates && Objects.equals(element, mmcsNode.element) && Objects.equals(candidatePredicates, mmcsNode.candidatePredicates) && Objects.equals(uncoverEvidenceSet, mmcsNode.uncoverEvidenceSet) && Objects.equals(crit, mmcsNode.crit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfPredicates, element, candidatePredicates, uncoverEvidenceSet, crit);
    }

}
