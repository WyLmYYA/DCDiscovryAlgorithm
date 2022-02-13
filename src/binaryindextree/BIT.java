package binaryindextree;

import Hydra.de.hpi.naumann.dc.helpers.ArrayIndexComparator;
import Hydra.de.hpi.naumann.dc.input.ParsedColumn;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.IEJoin;
import Hydra.de.hpi.naumann.dc.predicates.Operator;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import utils.TimeCal;

import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @Author yoyuan
 * @Description: Binary Index Tree
 * @DateTime: 2021/10/15 18:20
 */
public class BIT {

    private Cluster[] nodes;

    private ParsedColumn<?> ColC, ColD;

    private HashMap<Integer, int[]> hashMap = new HashMap<>();
    private int length;

    public IEJoin.Order order2;

    public Predicate p2;

    public BIT(int length, ParsedColumn<?> ColC, ParsedColumn<?> ColD) {
        this.ColC = ColC;
        this.ColD = ColD;
        this.nodes = new Cluster[length + 1];
        for (int i = 1; i <= length; ++i) {
            nodes[i] = new Cluster();
        }
        this.length = length + 1;
    }

    /**
     * add tupleId to index, we should add same id to manager of index
     */
    public void addTuple(int index, int tupleId) {
        while (index < length) {
            nodes[index].add(tupleId);
            index = index + lowbit(index);
        }
    }

    public IndexForBIT getSum(int rightBound, int value, Cluster cluster, IndexForBIT indexForBIT) {
        IndexForBIT res = new IndexForBIT();

        while (rightBound > 0) {
            long phase2 = System.currentTimeMillis();
            int[] arr;
            if (hashMap.containsKey(rightBound)){
                arr = hashMap.get(rightBound);
                TimeCal.add(System.currentTimeMillis() - phase2, 1);
            }
            else{
                arr = nodes[rightBound].toArray();
                hashMap.put(rightBound, arr);
            }

            int index = IEJoin.getOffset(value, arr, ColC.getIndex(), ColD.getIndex(), order2 == IEJoin.Order.DESCENDING,
                    p2.getOperator() == Operator.GREATER_EQUAL || p2.getOperator() == Operator.LESS_EQUAL);

            if(index != 0){
                res.offsetInNode.add(index);
                res.NodeIndex.add(rightBound);
            }

            rightBound -= lowbit(rightBound);
        }

        if (res.equals(indexForBIT)){
            return res;
        }else {

            for (int i = 0; i < res.NodeIndex.size(); ++i){
                int tmp = res.NodeIndex.get(i);
                int[] arr = hashMap.get(tmp);
                for(int j = 0; j < res.offsetInNode.get(i); ++j){
                    cluster.add(arr[j]);
                }
            }

        }
        return res;
    }

    public int getSum(int rightBound, int value, Cluster cluster, int pre) {
        int res = 0;
        boolean first = true;
        while (rightBound > 0) {
            if (first) {
                int[] arr = nodes[rightBound].toArray();
                int index = getOffset(arr, value);
                if (index == pre) return index;
                res = index;
                long timeForAddRes = System.currentTimeMillis();
                for (int i = 0; i < index; ++i) {
                    cluster.add(arr[i]);
                }
                TimeCal.add(System.currentTimeMillis() - timeForAddRes, 4);
            } else {
                long timeForAddRes = System.currentTimeMillis();
                cluster.addAll(nodes[rightBound]);
                TimeCal.add(System.currentTimeMillis() - timeForAddRes, 4);
            }
            rightBound -= lowbit(rightBound);
        }
        return res;
    }

    public List<Cluster> getSum(int rightBound) {
        List<Cluster> clusters = new ArrayList<>();
        while (rightBound > 0) {
            clusters.add(nodes[rightBound]);
            rightBound -= lowbit(rightBound);
        }
        return clusters;
    }

    private int getOffset(int[] arr, int value) {
        double val = (Double) ColC.getValue(value);
        if ((Double) ColD.getValue(arr[arr.length - 1]) < val) return arr.length;
        // get first one bigger than value
        int min = 0;
        int max = arr.length - 1;
        while (min < max) {
            int mid = (min + max) >>> 1;
            double midValue = (Double) ColD.getValue(arr[mid]);
            if (midValue > val) {
                max = mid;
            } else if (midValue <= val) {
                min = mid + 1;
            }
        }
        return min;
    }

    private int getOffsetDes(int[] arr, int value) {
        double val = (Double) ColC.getValue(value);
        if ((Double) ColD.getValue(arr[0]) < val) return arr.length;
        // get first one bigger than value
        int min = 0;
        int max = arr.length - 1;
        while (min < max) {
            int mid = (min + max) >>> 1;
            double midValue = (Double) ColD.getValue(arr[mid]);
            if (midValue > val) {
                max = mid;
            } else if (midValue <= val) {
                min = mid + 1;
            }
        }
        return min;
    }

    private int lowbit(int x) {
        return x & (-x);
    }

}
