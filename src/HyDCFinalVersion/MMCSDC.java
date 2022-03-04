package HyDCFinalVersion;


import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.ch.javasoft.bitset.search.NTreeSearch;
import Hydra.de.hpi.naumann.dc.algorithms.hybrid.ResultCompletion;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.PartitionEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.helpers.IndexProvider;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.IEJoin;
import Hydra.de.hpi.naumann.dc.paritions.LinePair;
import Hydra.de.hpi.naumann.dc.paritions.StrippedPartition;
import Hydra.de.hpi.naumann.dc.predicates.Operator;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.Closure;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import org.apache.lucene.util.RamUsageEstimator;
import utils.TimeCal2;

import java.util.*;

import static Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet.indexProvider;

/**
 * @Author yoyuan
 * @Description:  we treat mmcs process as a deep traversal of a tree,
 *                IEvidenceSet is evidence set to cover whose element is PredicateBitSet, a set of predicates = an evidence
 *
 * @DateTime: 2021/9/26 14:47
 */

public class MMCSDC {


    // 是否需要trans剪枝
    private final static boolean ENABLE_TRANSITIVE_CHECK = true;

    // 谓词数量
    private int numberOfPredicates;

    // 在MMCS的过程中直接收集dc,每个MMCSDC共享一份, 设为static
    public static DenialConstraintSet denialConstraintSet = new DenialConstraintSet();

    // 当前过程的候选谓词
    static IBitSet candidatePredicates;

    // 所有谓词的集合
    public static PredicateBuilder predicates;

    // 其他操作需要的配置
    public static Input input;

    public static IEJoin ieJoin;

    public static PartitionEvidenceSetBuilder partitionEvidenceSetBuilder;


    // 对trans剪枝的支撑树
    //TODO:  这里应该可以直接用MMCSNode替代
    NTreeSearch treeSearch = new NTreeSearch();

    // clusterPair共享树,在MMCS谓词顺序随机的时候需要，但是这样的话就会保持整棵树，这样内存肯定会爆的
    // 所以需要在MMCS的过程中谓词顺序按选择性来排序从上到下，谓词选择性从大道小
//    CPTree cpTree = new CPTree();

    // 支撑cpTree的clusterPair
//    public static List<ClusterPair> clusterPairs;


    public MMCSDC(int numberOfPredicates, IEvidenceSet evidenceSetToCover, PredicateBuilder predicates, Input input){

        this.numberOfPredicates = numberOfPredicates;
        ieJoin = new IEJoin(input.getInts());
        candidatePredicates = new LongBitSet(numberOfPredicates);

        this.predicates = predicates;
        this.input = input;

        for (int i = 0; i < numberOfPredicates; ++i){
            candidatePredicates.set(i);
        }

        List<ClusterPair> clusterPairs = new ArrayList<>();
        clusterPairs.add(StrippedPartition.getFullParition(input.getLineCount()));
//        cpTree.setClusterPairs(clusterPairs);

        initiate(evidenceSetToCover);

    }


    public void initiate(IEvidenceSet evidenceToCover){

        /**
         *   if there is evidenceSet is empty, return empty DC
        */
        walkDown(new MMCSNode(numberOfPredicates, evidenceToCover, input.getLineCount()));
    }

    public static long validTime = 0;

    public  HashEvidenceSet walkDown(MMCSNode currentNode){

        // minimize in mmcs,
        HashEvidenceSet ret = new HashEvidenceSet();
        if(ENABLE_TRANSITIVE_CHECK){
            long l1 = System.currentTimeMillis();
            for (int ne = currentNode.element.nextSetBit(0); ne != -1; ne = currentNode.element.nextSetBit(ne + 1)){
                IBitSet tmp = currentNode.element.clone();
                tmp.set(ne, false);
                for (Predicate p2 : indexProvider.getObject(ne).getInverse().getImplications()){
                    int index = indexProvider.getIndex(p2);
                    if (index < numberOfPredicates){
                        tmp.set(indexProvider.getIndex(p2));
                    }
                }

                if (treeSearch.containsSubset(tmp))return ret;
            }
            TimeCal2.add((System.currentTimeMillis() - l1), 1);
        }
        // 如果当前谓词集合能覆盖住当前的evidence集合，那么就是一个候选dc了，要对其进行valid
        if (currentNode.canCover()){

            // 如果是验证过后，并且已经用过clusterPair获得过完整evidence节点的子节点，那么就不需要其他操作，直接添加就可
            if(currentNode.needRefine){

//                currentNode.refineBySelectivity(cpTree);
                long l1 = System.currentTimeMillis();
                currentNode.refine();
                validTime += System.currentTimeMillis() - l1;
            }


            // 有时候clusterPair不为空，但是是无效的 //TODO:
            if (currentNode.isValidResult()){

                treeSearch.add(currentNode.element);

                denialConstraintSet.add(currentNode.getDenialConstraint());

            }else{
                // not a valid result means cluster pair not empty, we need get added evidence set
                // after this func, uncover update, and is a complete evidence for currNode, so cluster pair will be null

                HashEvidenceSet tmp = currentNode.getAddedEvidenceSet();
                ret.add(tmp);
                if (currentNode.uncoverEvidenceSet.size() == 0){
                    treeSearch.add(currentNode.element);

                    denialConstraintSet.add(currentNode.getDenialConstraint());
                }else
                    walkDown(currentNode);
            }
            return ret;
        }




        // 挑选下一个与candidate交集最小的evidence
        PredicateBitSet nextPredicates =  currentNode.getNextEvidence();

        // 当前节点的下一层的候选集为evidence和candidate的交集，但是下一层的候选集为差集
        IBitSet chosenEvidence = currentNode.candidatePredicates.getAnd(nextPredicates.getBitset());

        IBitSet nextCandidatePredicates = currentNode.candidatePredicates.getAndNot(chosenEvidence);

        // 按照选择性给谓词排序，保证在MMCS的时候是按照选择性大的谓词在上层的规则深度遍历
        List<Integer> sortedCandidates = getSortedCandidate(chosenEvidence);

        for (int next : sortedCandidates){
//        for (int next = chosenEvidence.nextSetBit(0); next >= 0; next = chosenEvidence.nextSetBit(next + 1)){

            // 对同组谓词进行剪枝，比如当前谓词为A==B,那么AXB都可以剪掉，X为任意operator
            IBitSet prunedCandidate = PruneNextPredicates(nextCandidatePredicates,next);

            // 子节点
//            System.out.println("used mem: " + (Runtime.getRuntime().totalMemory()) / (1024.0 * 1024) + "M");
            MMCSNode childNode = currentNode.getChildNode(next, prunedCandidate);
//            System.out.println("used mem: " + (Runtime.getRuntime().totalMemory()) / (1024.0 * 1024) + "M");

            if(childNode.isGlobalMinimal()){
                HashEvidenceSet tmp = walkDown(childNode);
                // 更新evidence，tmp为子节点新添加的evidence
                currentNode.uncoverEvidenceSet.add(tmp);
                ret.add(tmp);
                nextCandidatePredicates.set(next);
            }
        }
//        currentNode = null;
        return ret;


    }

    private IBitSet PruneNextPredicates(IBitSet nextCandidatePredicates, int next) {
        IBitSet tmp = nextCandidatePredicates.clone();

        Predicate predicate = indexProvider.getObject(next);

        Collection<Predicate> predicates =  predicate.getRedundants();

        // Triviality:
        predicates.forEach(predicate1 -> {
            int redundantIndex = indexProvider.getIndex(predicate1);
            if (redundantIndex < numberOfPredicates)
                tmp.set(indexProvider.getIndex(predicate1), false);
        });

        // Implication:
        predicate.getImplications().forEach(predicate1 -> {
            int redundantIndex = indexProvider.getIndex(predicate1);
            if (redundantIndex < numberOfPredicates)
                tmp.set(indexProvider.getIndex(predicate1), false);
        });

        return tmp;
    }

    private List<Integer> getSortedCandidate(IBitSet evidence){
        List<Integer> sortedCan = new ArrayList<>();
        for (int next = evidence.nextSetBit(0); next >= 0; next = evidence.nextSetBit(next + 1)){
            sortedCan.add(next);
        }
        // 这里要把选择性强的谓词放到后面，这样能保证选择性强的谓词先出现
        Collections.sort(sortedCan, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return indexProvider.getObject(o1).coverSize - indexProvider.getObject(o2).coverSize;

            }
        });
        return sortedCan;
    }

}
