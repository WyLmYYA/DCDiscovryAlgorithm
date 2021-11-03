package binaryindextree;

import Hydra.de.hpi.naumann.dc.paritions.Cluster;

/**
 * @Author yoyuan
 * @Description:
 * @DateTime: 2021/10/15 18:22
 */
public class BITNode {

    private Cluster cluster;


    public Cluster getCluster() {
        return cluster;
    }

    public int lowbit(int i){
        return i & (-i);
    }

    public void add(int i){
        cluster.add(i);
    }
}
