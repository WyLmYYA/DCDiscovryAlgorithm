package test;

import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.evidenceset.build.PartitionEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.input.InputIterationException;
import Hydra.de.hpi.naumann.dc.input.RelationalInput;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.IEJoin;
import Hydra.de.hpi.naumann.dc.predicates.Operator;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import gnu.trove.list.array.TIntArrayList;
import utils.TimeCal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TestIEJoinAndBITJoin {
    public  static void run(Input input, ClusterPair clusterPair, Predicate p1, Predicate p2){
        IEJoin ieJoin = new IEJoin(input);

        List<ClusterPair> resForBITJoin = new ArrayList<>();
        List<ClusterPair> resForIEJoin = new ArrayList<>();



        long l1 = System.currentTimeMillis();
        System.out.println("test for IEJoin");
        ieJoin.calc2ForTest(clusterPair, p1, p2, resForIEJoin);
        System.out.println("IEJoin Time " + (System.currentTimeMillis() - l1));
        System.out.println(resForIEJoin.stream().mapToLong(ClusterPair::getLinePairCount).sum());

        long l2 = System.currentTimeMillis();
        System.out.println("test for BITJoin");
        ieJoin.calcForTest(clusterPair, p1, p2, resForBITJoin);
        System.out.println("BITJoin Time " + (System.currentTimeMillis() - l2) );
        System.out.println(resForBITJoin.stream().mapToLong(ClusterPair::getLinePairCount).sum());
//                TimeCal.time.forEach(System.out::println);





    }

    public static void main(String[] args) throws IOException, InputIterationException {

        String line ="dataset//Tax10k.csv";
        int sizeLine = 10000;
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

        Predicate pre1 = null, pre2 = null;
        for (Predicate predicate : predicates1){
            if (predicate.getOperand1().getColumn().getIndex() == predicate.getOperand2().getColumn().getIndex())continue;
            if (predicate.getOperator() == Operator.GREATER || predicate.getOperator() == Operator.LESS ){
                if (pre1 == null)pre1 = predicate;
                else {
                    if(predicate.getOperand2() != pre1.getOperand2() && predicate.getOperand1() != pre1.getOperand1())
                    {
                        pre2 = predicate;
                        break;
                    }

                }
            }
        }
        run(input, clusterPair, pre1, pre2);

    }
}