package binaryindextree;

import Hydra.de.hpi.naumann.dc.paritions.Cluster;

import java.util.ArrayList;
import java.util.List;

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

    public int getSum(int rightBound, double value, Cluster cluster){
        int res = 0;
        boolean first = true;
        while(rightBound > 0){
            if(first){
                int[] arr = nodes[rightBound].toArray();
                int index = getOffset(arr, value);
                res = index;
                for(int i = 0; i < index; ++i) {
                    cluster.add(arr[i]);
                }
            }else {
                cluster.addAll(nodes[rightBound]);
            }

            rightBound -= lowbit(rightBound);
        }
        return res;
    }
    public List<Cluster> getSum(int rightBound){
        List<Cluster> clusters = new ArrayList<>();
        while(rightBound > 0){
            clusters.add(nodes[rightBound]);
            rightBound -= lowbit(rightBound);
        }
        return clusters;
    }

    private int getOffset(int[] arr, double value) {
        if(arr[arr.length - 1] < value) return arr.length;
        // get first one bigger than value
        int min =0;
        int max = arr.length-1;
        while(min<max){
            int mid = (min+max)>>>1;
            int midValue = arr[mid];
            if(midValue>value){
                max = mid;
            }else if(arr[mid]<=value){
                min = mid+1;
            }
        }
        return min;
    }

    private int lowbit(int x){
        return x & (-x);
    }

}
