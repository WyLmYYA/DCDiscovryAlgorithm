package HyDCFinalVersion;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.PartitionEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.paritions.*;
import Hydra.de.hpi.naumann.dc.predicates.*;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import utils.TimeCal2;

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


    private List<ClusterPair> clusterPairs;
//    public List<MMCSNode> nodesInPath = new ArrayList<>();
    private MMCSNode parentNode;

    public Predicate curPred;

    public boolean needRefine = true;

    private Predicate needCombinePre;



    public MMCSNode(int numberOfPredicates, IEvidenceSet evidenceToCover, int lineCount) {

        this.numberOfPredicates = numberOfPredicates;

        element = new LongBitSet();

        uncoverEvidenceSet = (HashEvidenceSet) evidenceToCover;

        candidatePredicates = MMCSDC.candidatePredicates;

        clusterPairs = new ArrayList<>();
        clusterPairs.add(StrippedPartition.getFullParition(lineCount));

        crit = new ArrayList<>(numberOfPredicates);
        for (int i = 0; i < numberOfPredicates; ++i){
            crit.add(new ArrayList<>());
        }
//        nodesInPath.add(this);

    }

    public MMCSNode(int numberOfPredicates) {

        this.numberOfPredicates = numberOfPredicates;

    }

    public boolean canCover() {
        return uncoverEvidenceSet.isEmpty();
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
        for (PredicateBitSet predicates : node.uncoverEvidenceSet.getSetOfPredicateSets()) {
            if(predicates.getBitset().get(predicateAdded)) crit.get(predicateAdded).add(predicates);
            else uncoverEvidenceSet.add(predicates);
        }
//        node.uncoverEvidenceSet.getSetOfPredicateSets().forEach(predicates -> {
//            if(predicates.getBitset().get(predicateAdded)) crit.get(predicateAdded).add(predicates);
//            else uncoverEvidenceSet.add(predicates);
//        });

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

        needRefine = parentNode.needRefine;

//        nodesInPath.add(this);



    }

    // MMCS中加入了谓词选择，那么MMCS的深度遍历顺序就是选择性从高到低，那么共享的clusterPair就肯定在一条路上
    // 1、可以控制内存中只保持一条路的内存使用
    // 2、选择性可以让前面的clusterPair尽快变小
    // 3、尽可能多的共享深度遍历前面已有的clusterPair
    // 当一个集合是MMCS的一个覆盖时，我们进行refine，也就是验证

    public static int allValid = 0, sharedNum = 0;
    public void refine(){
        // 先找到第一个clusterPair不为空的父节点
        MMCSNode firstParent = this;
        List<MMCSNode> needRefined = new ArrayList<>();
        int num = 0;
        while(firstParent.clusterPairs == null){
            // 按照MMCS遍历的顺序插入列表，所以父节点是插入头部
            needRefined.add(0, firstParent);
            firstParent = firstParent.parentNode;
        }

        // 用来记录上一个节点的clusterPair
        List<ClusterPair> curClusterPairs = firstParent.clusterPairs;

        sharedNum += this.element.cardinality() - curClusterPairs.size();

        allValid ++;
        // 父节点遗留的不等谓词
        Predicate preNeedCombine = firstParent.needCombinePre;

        for (MMCSNode mmcsNode : needRefined){
            // 如果是==，<>直接进行refine
            if (!mmcsNode.curPred.needCombine()){
                List<ClusterPair> newRes = new ArrayList<>();
                curClusterPairs.forEach(clusterPair -> {
                    clusterPair.refinePsPublic(mmcsNode.curPred.getInverse(), MMCSDC.ieJoin, newRes);
                });
                mmcsNode.clusterPairs = newRes;
                if (preNeedCombine != null) mmcsNode.needCombinePre = preNeedCombine;
            }else{
                // 如果是不等谓词，需要看前面是否有落单的不等谓词
                if (preNeedCombine == null){
                    // 如果没有，那么继承父节点的clusterPair
                    mmcsNode.needCombinePre = mmcsNode.curPred;
                    preNeedCombine = mmcsNode.curPred;
                    mmcsNode.clusterPairs = curClusterPairs;
                }else{
                    // 如果刚好可以凑成一对
                    List<ClusterPair> newRes = new ArrayList<>();
                    Predicate finalPreNeedCombine = preNeedCombine;
                    curClusterPairs.forEach(clusterPair -> {
                        clusterPair.refinePPPublic(new PredicatePair(finalPreNeedCombine.getInverse(), mmcsNode.curPred.getInverse()), MMCSDC.ieJoin, clusterPair1 -> newRes.add(clusterPair1));
                    });
                    mmcsNode.clusterPairs = newRes;
                    preNeedCombine = null;
                }
            }
            // 改变引用对象
            curClusterPairs = mmcsNode.clusterPairs;
        }
        if (preNeedCombine != null) {
            List<ClusterPair> newRes = new ArrayList<>();
            curClusterPairs.forEach(clusterPair -> {
                clusterPair.refinePsPublic(needCombinePre.getInverse(), MMCSDC.ieJoin, newRes);
            });
            clusterPairs = newRes;
            this.needCombinePre = null;
        }




    }
//    public void refineBySelectivity(CPTree cpTree){
////        List<Predicate> needCombination = new ArrayList<>();
//        List<Predicate> refiners = new ArrayList<>();
////        HashMap<PartitionRefiner, Integer> selectivity = new HashMap<>();
//        MMCSNode tmp = this;
//
//        // get all nodes in path
//        while (tmp.curPred != null && tmp != null){
//            refiners.add(tmp.curPred);
//            tmp = tmp.parentNode;
//        }
//
//        // sort
//        long l1 = System.currentTimeMillis();
//        sortPredicate(refiners);
//        // find subset
//        CPTree cpTree1 = cpTree.add(refiners, 0);
//        if (cpTree1.getNeedCombine() != null){
//            List<ClusterPair> newResult = new ArrayList<>();
//            cpTree1.getClusterPairs().forEach(clusterPair -> {
//                clusterPair.refinePsPublic(cpTree1.getNeedCombine().getInverse(), MMCSDC.ieJoin, newResult);
//            });
//            cpTree1.setClusterPairs(newResult);
//            cpTree1.setNeedCombine(null);
//            MMCSDC.clusterPairs = newResult;
//        }else
//            MMCSDC.clusterPairs = cpTree1.getClusterPairs();
//        TimeCal2.add((System.currentTimeMillis() - l1), 0);
//
//    }
    private void sortPredicate(List<Predicate> list){
        Collections.sort(list, new Comparator<Predicate>() {
            @Override
            public int compare(Predicate o1, Predicate o2) {
                return o2.coverSize - o1.coverSize;

            }
        });
    }

    public static long pairTime = 0;


    private void cloneContext(IBitSet nextCandidatePredicates, MMCSNode parentNode) {
        element = parentNode.element.clone();

        candidatePredicates = nextCandidatePredicates.clone();

        crit = new ArrayList<>(numberOfPredicates);

//        parentNode.nodesInPath.forEach(mmcsNode -> nodesInPath.add(mmcsNode));
        this.parentNode = parentNode;
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

    public HashEvidenceSet getAddedEvidenceSet(){
        HashEvidenceSet newEvi = new HashEvidenceSet();


        long l2 = System.currentTimeMillis();

        if (MMCSDC.partitionEvidenceSetBuilder == null){
            MMCSDC.partitionEvidenceSetBuilder = new PartitionEvidenceSetBuilder(MMCSDC.predicates, MMCSDC.input.getInts());
        }
        for (ClusterPair clusterPair : clusterPairs){

            MMCSDC.partitionEvidenceSetBuilder.addEvidencesForHyDC(clusterPair, newEvi);
        }
        TimeCal2.add((System.currentTimeMillis() - l2), 1);


        Iterator iterable = newEvi.getSetOfPredicateSets().iterator();
        while (iterable.hasNext()){
            PredicateBitSet predicates1 = (PredicateBitSet) iterable.next();
            IBitSet tmp = predicates1.getBitset().getAnd(element);
            if (tmp.cardinality() != 0){
                iterable.remove();
                if (tmp.cardinality() == 1){
                    crit.get(tmp.nextSetBit(0)).add(predicates1);
                }
            }
        }

        uncoverEvidenceSet.add(newEvi);
        clusterPairs = null;
        needRefine = false;
        return newEvi;
    }
    public boolean isValidResult(){
        // there may be {12} : {12, 12}, so need to add one step to judge
        if (clusterPairs == null)return true;
        for (ClusterPair clusterPair : clusterPairs){
//            Iterator<LinePair> iter = clusterPair.getLinePairIterator();
//            while (iter.hasNext()) {
//                LinePair lPair = iter.next();
//                int i = lPair.getLine1();
//                int j = lPair.getLine2();
//                if (i != j) return false;
//            }
            if(clusterPair.containsLinePair()){
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
