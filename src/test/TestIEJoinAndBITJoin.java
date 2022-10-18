package test;

import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.evidenceset.build.PartitionEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.input.InputIterationException;
import Hydra.de.hpi.naumann.dc.input.RelationalInput;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.IEJoin;
import Hydra.de.hpi.naumann.dc.paritions.LinePair;
import Hydra.de.hpi.naumann.dc.predicates.Operator;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.operands.ColumnOperand;
import gnu.trove.list.array.TIntArrayList;
import utils.TimeCal;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class TestIEJoinAndBITJoin {
    public  static void run(Input input, ClusterPair clusterPair, Predicate p1, Predicate p2){
        IEJoin ieJoin = new IEJoin(input);

        List<ClusterPair> resForBITJoin = new ArrayList<>();
        List<ClusterPair> resForIEJoin = new ArrayList<>();

        Set<LinePair> iejoinSet = new HashSet<>();


        long l1 = System.currentTimeMillis();
        System.out.println("test for IEJoin");
        ieJoin.calc(clusterPair, p1, p2, clusterPair1 -> resForIEJoin.add(clusterPair1));
        System.out.println("IEJoin Time " + (System.currentTimeMillis() - l1));
        int sum1 = (int) resForIEJoin.stream().mapToLong(ClusterPair::getLinePairCount).sum();
        System.out.println(resForIEJoin.stream().mapToLong(ClusterPair::getLinePairCount).sum());


        long l2 = System.currentTimeMillis();
        System.out.println("test for BITJoin");
        ieJoin.calcForBIT(clusterPair, p1, p2, clusterPair1 -> resForBITJoin.add(clusterPair1));
        System.out.println("BITJoin Time " + (System.currentTimeMillis() - l2) );
//        int sum2 = (int)resForBITJoin.stream().mapToLong(ClusterPair::getLinePairCount).sum();
        System.out.println(resForBITJoin.stream().mapToLong(ClusterPair::getLinePairCount).sum());
//                TimeCal.time.forEach(System.out::println);

//        for (int i = 0; i < resForBITJoin.size(); ++i){
//            Iterator<LinePair> iter1 = resForBITJoin.get(i).getLinePairIterator();
//            while (iter1.hasNext()) {
//                LinePair lPair = iter1.next();
//                if (!iejoinSet.contains(lPair)){
//                    System.out.println(lPair);
//                }
//            }
//        }
//        iejoinSet.forEach(System.out::println);
//
//        if (sum1 != sum2){
//            System.out.println(p1);
//            System.out.println(p2);
//            System.out.println(sum1);
//            System.out.println(sum2);
//        }





    }

    public static void main(String[] args) throws IOException, InputIterationException {

        int[] sizes = new int[]{600000};
        for(int j = 0; j < sizes.length; ++j){
            String line ="dataset//ncvoter.csv";
            int sizeLine = sizes[j];
//        String line ="dataset//Test.csv";
//        int sizeLine = 7;
            int size=Integer.valueOf(sizeLine);
            File datafile = new File(line);

            RelationalInput data = new RelationalInput(datafile);
            Input input = new Input(data,size);
            PredicateBuilder predicates = new PredicateBuilder(input, false, 0.3d);
            System.out.println("predicate space:"+predicates.getPredicates().size());

            int[] clu = new int[sizeLine];
            for (int i = 0; i < sizeLine; ++i){
                clu[i] = i;
            }
            Cluster cluster1 = new Cluster(new TIntArrayList(clu));
            Cluster cluster2 = new Cluster(new TIntArrayList(clu));
            ClusterPair clusterPair = new ClusterPair(cluster1, cluster2);
            List<Predicate> predicates1 = new ArrayList<>(predicates.getPredicates());

            Predicate pre1 = new Predicate(Operator.GREATER,new ColumnOperand<>(input.getColumns()[12], 12),
                    new ColumnOperand<>(input.getColumns()[50], 50) );
            Predicate pre2 = new Predicate(Operator.LESS,new ColumnOperand<>(input.getColumns()[12], 12),
                    new ColumnOperand<>(input.getColumns()[48], 48) );
            System.out.println(pre1);
            System.out.println(pre2);
            run(input, clusterPair, pre1, pre2);
        }




    }
}
