//package HyDCNoSort;
//
//import Hydra.ch.javasoft.bitset.IBitSet;
//import Hydra.ch.javasoft.bitset.LongBitSet;
//import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
//import Hydra.de.hpi.naumann.dc.paritions.Cluster;
//import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
//import Hydra.de.hpi.naumann.dc.paritions.IEJoin;
//import Hydra.de.hpi.naumann.dc.predicates.Predicate;
//import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
//import gnu.trove.list.array.TIntArrayList;
//import utils.TimeCal;
//
//import java.util.*;
//
///**
// * @Author yoyuan
// * @Description:   tree node in the deep transversal of MMCS for DC
// * @DateTime: 2021/9/26 14:56
// */
//public class MMCSHyDCNode {
//
//    private int numberOfPredicates;
//    /**
//     *  the set of predicates which can cover the evidence set
//    */
//    private IBitSet element;
//
//    IBitSet candidatePredicates;
//
//    public HashEvidenceSet uncoverEvidenceSet;
//
//    private List<List<PredicateBitSet>> crit;
//
//    public IBitSet getElement() {
//        return element;
//    }
//
//    /**
//     * @Author yongan.yuan
//     * @Description: add attributes for HyDC
//     */
//
//    // if predicate of node is not equal, we need to combine another to run BITJoin
//    public boolean isNeedCombine = false;
//
//    public boolean isCombineWithParent = false;
//
//    public int numOfNeedCombinePredicate;
//
//    public List<ClusterPair> clusterPairs;
//
//    // new added evidenceSet from childNode, initial is null
//    public HashEvidenceSet newEvidenceSet = null;
//
//    public Predicate lastPredicate;
//
//    // maintain evidence set from beg to end
//    public HashEvidenceSet completeEvidenceSet;
//
//
//    public MMCSHyDCNode( int lineCount, int numberOfPredicates, HashEvidenceSet evidenceToCover, int numOfNeedCombinePredicate ) {
//
//        this.numberOfPredicates = numberOfPredicates;
//
//        this.numOfNeedCombinePredicate = numOfNeedCombinePredicate;
//
//        element = new LongBitSet();
//
//        uncoverEvidenceSet = evidenceToCover;
//
//        completeEvidenceSet = evidenceToCover;
//
//        //Initial ClusterPair only one full lineCount
//        TIntArrayList c = new TIntArrayList();
//        for(int i = 0; i < lineCount; ++i){
//            c.add(i);
//        }
//        clusterPairs = new ArrayList<>();
//        clusterPairs.add(new ClusterPair(new Cluster(c), new Cluster(c)));
//
//        candidatePredicates = MMCSHyDC.candidatePredicates;
//
//        newEvidenceSet = new HashEvidenceSet();
//        crit = new ArrayList<>(numberOfPredicates);
//        for (int i = 0; i < numberOfPredicates; ++i){
//            crit.add(new ArrayList<>());
//        }
//    }
//
//    public MMCSHyDCNode(int numberOfPredicates) {
//
//        this.numberOfPredicates = numberOfPredicates;
//
//        newEvidenceSet = new HashEvidenceSet();
//    }
//
//    public boolean canCover() {
//        return uncoverEvidenceSet.isEmpty();
//    }
//
//
//    public PredicateBitSet getNextEvidence() {
//
//        Comparator<PredicateBitSet> cmp = Comparator.comparing(predicates -> predicates.getBitset().getAnd(candidatePredicates));
//
//        return Collections.min(uncoverEvidenceSet.getSetOfPredicateSets(), cmp);
//
//    }
//
//    public MMCSHyDCNode getChildNode(int predicateAdded, IBitSet nextCandidatePredicates, IEJoin ieJoin) {
//
//        MMCSHyDCNode childNode = new MMCSHyDCNode(numberOfPredicates);
//
//        long l2 = System.currentTimeMillis();
//        childNode.cloneContext(nextCandidatePredicates, this);
//        TimeCal.add((System.currentTimeMillis() - l2), 1);
//
//
//        long l3 = System.currentTimeMillis();
//        boolean isGlobalMini = childNode.updateContextFromParent(predicateAdded, this);
//        TimeCal.add((System.currentTimeMillis() - l3), 2);
//
//        if (!isGlobalMini) return null;
//        long l1 = System.currentTimeMillis();
//        // update clusterPair BITJoin is here
//        childNode.refine(this, ieJoin);
//        TimeCal.add((System.currentTimeMillis() - l1), 0);
//
//        return childNode;
//
//    }
//
//    // TODO: 目前只支持大于小于，还没加大于等于以及小于等于
//    private void refine(MMCSHyDCNode parentNode, IEJoin ieJoin){
//        List<ClusterPair> newResult = new ArrayList<>();
////        if (parentNode.isNeedCombine && this.isNeedCombine && !parentNode.isCombineWithParent){
////            // refine use BITJoin,
////            for (ClusterPair clusterPair : clusterPairs){
////                ieJoin.calcForTest(clusterPair,
////                        parentNode.lastPredicate.getInverse(),
////                        this.lastPredicate.getInverse(),
////                        newResult
////                );
////            }
////            this.isCombineWithParent = true;
////        }
////        else if ( (!parentNode.isNeedCombine || (parentNode.isNeedCombine && parentNode.isCombineWithParent) ) && this.isNeedCombine){
////            // wait next combine
////            return;
////        }
////        else{
////            // use normal way
////            if (parentNode.isNeedCombine && !parentNode.isCombineWithParent){
////                List<ClusterPair> newResults = new ArrayList<>();
////                for (ClusterPair clusterPair : parentNode.clusterPairs) {
////                    clusterPair.refine(parentNode.lastPredicate.getInverse(), ieJoin, newResults::add);
////                }
////                parentNode.clusterPairs = newResults;
////            }
////            clusterPairs = new ArrayList<>(parentNode.clusterPairs);
////            for (ClusterPair clusterPair : clusterPairs) {
////                clusterPair.refine(lastPredicate.getInverse(), ieJoin, newResult::add);
////            }
////        }
//
//        // only normal join for single predicate
//        for (ClusterPair clusterPair : clusterPairs) {
//            clusterPair.refinePsPublic(lastPredicate.getInverse(), ieJoin, newResult);
//        }
//
//        // Result may be duplicated, however can't refine
////        refineClusterPairs(newResult);
//
//        clusterPairs = newResult;
////        clusterPairs.stream().filter(clusterPair -> !clusterPair.isEmpty());
//    }
//    private boolean updateContextFromParent(int predicateAdded, MMCSHyDCNode node) {
//
//        //TODO: we can change TroveEvidenceSet to see which one is more suitable
//        uncoverEvidenceSet = new HashEvidenceSet();
//
//        /**
//         *  after add new Predicates, some uncoverEvidenceSet can be removed, so we don't need add them to childNode
//         *  and we should update crit meanwhile
//        */
//        node.uncoverEvidenceSet.getSetOfPredicateSets().forEach(predicates -> {
//            if(predicates.getBitset().get(predicateAdded)) crit.get(predicateAdded).add(predicates);
//            else uncoverEvidenceSet.add(predicates);
//        });
//
//        /**
//         *  update crit in terms of  the EvidenceSet which is covered by predicateAdded
//        */
//        for (int next = element.nextSetBit(0); next >= 0; next = element.nextSetBit(next + 1))
//            crit.get(next).removeIf(predicates -> predicates.getBitset().get(predicateAdded));
//
//        if (!this.isGlobalMinimal()){
//            return false;
//        }
//
//        /**
//         *  update element
//        */
//        element.set(predicateAdded, true);
//
//        lastPredicate = PredicateBitSet.getPredicate(predicateAdded);
//
//        if (lastPredicate.needCombine()) {
//            isNeedCombine = true;
//            numOfNeedCombinePredicate -= 1;
//        }
//
//        return true;
//
//    }
//
//    private void cloneContext(IBitSet nextCandidatePredicates, MMCSHyDCNode parentNode) {
//
//
//        /** clone don't time */
//        element = parentNode.element.clone();
//
//        candidatePredicates = nextCandidatePredicates.clone();
//
//        numOfNeedCombinePredicate = parentNode.numOfNeedCombinePredicate;
//
//        clusterPairs = new ArrayList<>(parentNode.clusterPairs);
//
//
//
////        completeEvidenceSet = new HashEvidenceSet();
////        completeEvidenceSet.add(parentNode.completeEvidenceSet);
//        completeEvidenceSet = parentNode.completeEvidenceSet;
//
//
//        crit = new ArrayList<>(numberOfPredicates);
//        for (int i = 0; i < numberOfPredicates; ++i){
//            crit.add(new ArrayList<>(parentNode.crit.get(i)));
//        }
//
//
//    }
//
//    /**
//     *  Judge current node is global minimal or not by crit
//    */
//    public boolean isGlobalMinimal() {
//        for(int next = element.nextSetBit(0); next >= 0; next = element.nextSetBit(next+1))
//            if(crit.get(next).isEmpty()) return false;
//        return true;
//    }
//
//    public void refineClusterPairs(List<ClusterPair> result){
//        Map<Integer, Set<Integer>> map = new HashMap<>();
//        result.removeIf(clusterPair -> {
//            Cluster c1 = clusterPair.getC1();
//            Cluster c2 = clusterPair.getC2();
//            for (int c1Val : c1.toArray()){
//                if (!map.containsKey(c1Val)){
//                    map.put(c1Val, c2.toSet());
//                    continue;
//                }
//                Set<Integer> tmp = map.get(c1Val);
//                for (int c2Val : c2.toArray()){
//                    if (tmp.add(c2Val)){
//                        c2.remove(c2Val);
//                    }
//                }
//            }
//            return !clusterPair.containsLinePair();
//        });
//    }
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        MMCSHyDCNode mmcsNode = (MMCSHyDCNode) o;
//        return numberOfPredicates == mmcsNode.numberOfPredicates && Objects.equals(element, mmcsNode.element) && Objects.equals(candidatePredicates, mmcsNode.candidatePredicates) && Objects.equals(uncoverEvidenceSet, mmcsNode.uncoverEvidenceSet) && Objects.equals(crit, mmcsNode.crit);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(numberOfPredicates, element, candidatePredicates, uncoverEvidenceSet, crit);
//    }
//
//}
