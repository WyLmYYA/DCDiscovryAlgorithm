package binaryindextree;

import Hydra.de.hpi.naumann.dc.paritions.Cluster;

/**
 * @Author yoyuan
 * @Description: Binary Index Tree
 * @DateTime: 2021/10/15 18:20
 */
public class BIT {

    private Cluster[] nodes;

    private int length;

    public BIT(int length) {
        this.nodes = new Cluster[length + 1];
        for(int i = 1; i <= length; ++i){
            nodes[i] = new Cluster();
        }
        this.length = length + 1;
    }

    /** add tupleId to index, we should add same id to manager of index*/
    public void addTuple(int index, int tupleId){
        while(index < length){
            nodes[index].add(tupleId);
            index = index + lowbit(index);
        }
    }

    public Cluster getSum(int rightBound){
        Cluster cluster = new Cluster();
        while(rightBound > 0){
            cluster.addAll(nodes[rightBound]);
            rightBound -= lowbit(rightBound);
        }
        return cluster;
    }

    private int lowbit(int x){
        return x & (-x);
    }

}
