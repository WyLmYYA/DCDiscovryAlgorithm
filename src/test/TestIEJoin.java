package test;

import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.evidenceset.build.PartitionEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.input.InputIterationException;
import Hydra.de.hpi.naumann.dc.input.RelationalInput;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.IEJoin;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TestIEJoin {
    public  static void run(Input input, ClusterPair clusterPair, Predicate p1, Predicate p2){
        IEJoin ieJoin = new IEJoin(input);

        List<ClusterPair> resForBITJoin = new ArrayList<>();
        List<ClusterPair> resForIEJoin = new ArrayList<>();
//        long l1 = System.currentTimeMillis();
//        System.out.println("test for IEJoin");
//        ieJoin.calc2ForTest(clusterPair, p1, p2, resForIEJoin);
//        System.out.println("IEJoin Time " + (System.currentTimeMillis() - l1));

        long l2 = System.currentTimeMillis();
        System.out.println("test for BITJoin");
        ieJoin.calcForTest(clusterPair, p1, p2, resForBITJoin);
        System.out.println("BITJoin Time " + (System.currentTimeMillis() - l2) );


    }

    public static void main(String[] args) throws IOException, InputIterationException {

        String line ="dataset//Tax10k.csv";
        int sizeLine = 10000;
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
        run(input, clusterPair, predicates1.get(2), predicates1.get(3));

    }
}
