package HyDC;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.helpers.IndexProvider;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.IEJoin;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import gnu.trove.list.array.TIntArrayList;

import javax.swing.plaf.basic.BasicListUI;
import java.util.*;
import java.util.function.Consumer;

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

    // if predicate of node is not equal, we need to combine another to run BITJoin
    public boolean isNeedCombine = false;

    public int numOfNeedCombinePredicate;

    public List<ClusterPair> clusterPairs;

    // new added evidenceSet from childNode, initial is null
    public HashEvidenceSet newEvidenceSet = null;

    private Predicate lastPredicate;


    public MMCSHyDCNode( int lineCount, int numberOfPredicates, HashEvidenceSet evidenceToCover, int numOfNeedCombinePredicate ) {

        this.numberOfPredicates = numberOfPredicates;

        this.numOfNeedCombinePredicate = numOfNeedCombinePredicate;

        element = new LongBitSet();

        uncoverEvidenceSet = evidenceToCover;

        //Initial ClusterPair only one full lineCount
        TIntArrayList c = new TIntArrayList();
        for(int i = 0; i < lineCount; ++i){
            c.add(i);
        }
        clusterPairs.add(new ClusterPair(new Cluster(c), new Cluster(c)));

        candidatePredicates = MMCSHyDC.candidatePredicates;

        crit = new ArrayList<>(numberOfPredicates);
        for (int i = 0; i < numberOfPredicates; ++i){
            crit.add(new ArrayList<>());
        }
    }

    public MMCSHyDCNode(int numberOfPredicates) {

        this.numberOfPredicates = numberOfPredicates;

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
        // update clusterPair BITJoin is here
        childNode.refine(this, ieJoin);

        return childNode;

    }

    private void refine(MMCSHyDCNode parentNode, IEJoin ieJoin){
        List<ClusterPair> newResult = new ArrayList<>();
        if (parentNode.isNeedCombine && this.isNeedCombine){
            // refine use BITJoin,
            for (ClusterPair clusterPair : clusterPairs){
                ieJoin.calcForBITJoin(clusterPair,
                        parentNode.lastPredicate,
                        this.lastPredicate,
                        newResult
                );
            }

        }else if (!parentNode.isNeedCombine && this.isNeedCombine && numOfNeedCombinePredicate >= 1){
            // wait next combine
            return;
        }else{
            // use normal way
            for (ClusterPair clusterPair : clusterPairs) {
                clusterPair.refinePsPublic(lastPredicate, ieJoin, newResult);
            }
        }
        clusterPairs = newResult;
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

        lastPredicate = PredicateBitSet.getPredicate(predicateAdded);

        if (lastPredicate.needCombine()) numOfNeedCombinePredicate -= 1;

        return true;

    }

    private void cloneContext(IBitSet nextCandidatePredicates, MMCSHyDCNode parentNode) {
        element = parentNode.element.clone();

        candidatePredicates = nextCandidatePredicates.clone();

        numOfNeedCombinePredicate = parentNode.numOfNeedCombinePredicate;

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
        MMCSHyDCNode mmcsNode = (MMCSHyDCNode) o;
        return numberOfPredicates == mmcsNode.numberOfPredicates && Objects.equals(element, mmcsNode.element) && Objects.equals(candidatePredicates, mmcsNode.candidatePredicates) && Objects.equals(uncoverEvidenceSet, mmcsNode.uncoverEvidenceSet) && Objects.equals(crit, mmcsNode.crit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfPredicates, element, candidatePredicates, uncoverEvidenceSet, crit);
    }
}
