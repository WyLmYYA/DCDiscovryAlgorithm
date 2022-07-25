package HyDCFinalVersion;

import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicatePair;
import utils.TimeCal2;

import java.util.*;

/**
 * @Author yoyuan
 * @Description:
 * @DateTime: 2022/1/20 12:06
 */
public class CPTree {
    private Predicate curPredicate;

    private HashMap<Predicate, CPTree> children = new HashMap<>();

    private List<ClusterPair> clusterPairs;

    private Predicate needCombine;

    public boolean recentUsed = false;

    public CPTree(Predicate curPredicate) {
        this.curPredicate = curPredicate;
    }

    public CPTree(){}

    public CPTree(List<ClusterPair> clusterPairs){
        this.clusterPairs = clusterPairs;
    }

    public void setClusterPairs(List<ClusterPair> clusterPairs){
        this.clusterPairs = clusterPairs;
    }
    public CPTree add(List<Predicate> addList, int next){
        if (next == addList.size()) return this;
        Predicate nextPre = addList.get(next);
        if (children.containsKey(nextPre)){
            CPTree child = children.get(nextPre);
            child.recentUsed = true;
            return child.add(addList, next + 1);
        }else{
            if(MMCSNode.curShareLen == -1)MMCSNode.curShareLen = next + 1;
            CPTree cpTree = new CPTree(nextPre);
            List<ClusterPair> newResult = new ArrayList<>();
//            clusterPairs.forEach(clusterPair -> {
//                clusterPair.refinePsPublic(nextPre.getInverse(), MMCSDC.ieJoin, newResult);
//            });
//            cpTree.needCombine = needCombine;
            if (nextPre.needCombine() && next >= 5){
                if (needCombine == null){
                    cpTree.needCombine = nextPre;
                    cpTree.clusterPairs = clusterPairs;
                    children.put(nextPre, cpTree);
                    return cpTree.add(addList, next + 1);
                }else {
                    long l1 = System.currentTimeMillis();
                    clusterPairs.forEach(clusterPair -> {
                        clusterPair.refinePPPublic(new PredicatePair(needCombine.getInverse(), nextPre.getInverse()), MMCSDC.ieJoin, clusterPair1 -> newResult.add(clusterPair1));
                    });
                    TimeCal2.add((System.currentTimeMillis() - l1), 2);
                }

            }else{
                long l1 = System.currentTimeMillis();
                clusterPairs.forEach(clusterPair -> {
                    clusterPair.refinePsPublic(nextPre.getInverse(), MMCSDC.ieJoin, newResult);
                });
                cpTree.needCombine = needCombine;
                TimeCal2.add((System.currentTimeMillis() - l1), 2);
            }

            cpTree.clusterPairs = newResult;
            children.put(nextPre, cpTree);
            return cpTree.add(addList, next + 1);
        }
    }

    public void setNeedCombine(Predicate predicate){
        this.needCombine = predicate;
    }
    public List<ClusterPair> getClusterPairs(){
        return clusterPairs;
    }
    public Predicate getNeedCombine(){
        return needCombine;
    }

    public void LRU(){
        Iterator<Predicate> iter = children.keySet().iterator();
        while(iter.hasNext()){
            Predicate predicate = iter.next();
            CPTree cpTree = children.get(predicate);
            if (!cpTree.recentUsed){
                cpTree.deleteCurrentNode();
                iter.remove();
            }else cpTree.recentUsed = false;

        }
    }
    public void deleteCurrentNode(){
        Iterator<Predicate> iter = children.keySet().iterator();
        while (iter.hasNext()){
            Predicate predicate = iter.next();
            CPTree cpTree = children.get(predicate);
            cpTree.deleteCurrentNode();
            iter.remove();
        }
    }
}
