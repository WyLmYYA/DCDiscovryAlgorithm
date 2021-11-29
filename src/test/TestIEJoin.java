package test;

import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.evidenceset.build.PartitionEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.IEJoin;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;

import java.util.List;
import java.util.function.Consumer;

public class TestIEJoin {
    public static void run(Input input, ClusterPair clusterPair, Predicate p1, Predicate p2, Consumer<ClusterPair> consumer){
        IEJoin ieJoin = new IEJoin(input);
//        PartitionEvidenceSetBuilder builder = new PartitionEvidenceSetBuilder(predicates, values);
//        Consumer<ClusterPair> consumer = (clusterPair) -> {
//            List<DenialConstraint> currentDCs = predicateDCMap.get(inter.currentBits);
//            if (currentDCs != null) {
//                // EtmPoint point = etmMonitor.createPoint("EVIDENCES");
//                builder.addEvidences(clusterPair, resultEv);
//                // point.collect();
//            } else {
//                inter.nextRefiner.accept(clusterPair);
//            }
//        };
//        ieJoin.calc(clusterPair, p1, p2, consumer );
    }
}
