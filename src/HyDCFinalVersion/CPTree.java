package HyDCFinalVersion;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.search.NTreeSearch;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import utils.TimeCal2;

import java.util.*;

/**
 * @Author yoyuan
 * @Description:
 * @DateTime: 2022/1/20 12:06
 */
public class CPTree {
//    private Predicate curPredicate;

    private HashMap<Predicate, CPTree> children = new HashMap<>();

    private List<ClusterPair> clusterPairs;

//    public CPTree(Predicate curPredicate) {
//        this.curPredicate = curPredicate;
//    }
    public CPTree(){}
    public CPTree(List<ClusterPair> clusterPairs){
        this.clusterPairs = clusterPairs;
    }

    public void setClusterPairs(List<ClusterPair> clusterPairs){
        this.clusterPairs = clusterPairs;
    }
    public List<ClusterPair> add(List<Predicate> addList, int next){
        if (next == addList.size()) return clusterPairs;
        Predicate nextPre = addList.get(next);
        if (children.containsKey(nextPre)){
            return children.get(nextPre).add(addList, next + 1);
        }else{
            CPTree cpTree = new CPTree();
            List<ClusterPair> newResult = new ArrayList<>();
            for (ClusterPair clusterPair : clusterPairs) {
                TimeCal2.add(1,4);
                clusterPair.refinePsPublic(nextPre.getInverse(), MMCSDC.ieJoin, newResult);
            }
//            clusterPairs.forEach(clusterPair -> {
//                TimeCal2.add(1,4);
//                clusterPair.refinePsPublic(nextPre.getInverse(), MMCSDC.ieJoin, newResult);
//            });
            cpTree.clusterPairs = newResult;
            children.put(nextPre, cpTree);
            return cpTree.add(addList, next + 1);
        }
    }


}
