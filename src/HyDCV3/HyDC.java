package HyDCV3;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.de.hpi.naumann.dc.algorithms.hybrid.ResultCompletion;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.PartitionEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.SystematicLinearEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.StrippedPartition;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet.indexProvider;

/**
 * @Author yoyuan
 * @Description:
 * @DateTime: 2021/12/18 23:15
 */
public class HyDC {
    PartitionEvidenceSetBuilder partitionEvidenceSetBuilder;

    int numberOfPredicates;

    Input input;

    PredicateBuilder predicates;

    HashEvidenceSet initSampleEvidence;

    HashEvidenceSet fullEvidence;

    HashEvidenceSet newAddedEvidence = new HashEvidenceSet();

//    HashEvidenceSet newAdded2Evidence = new HashEvidenceSet();
    PartitionEvidenceSetBuilder builder;

    public void run(PredicateBuilder predicates, IEvidenceSet sampleEvidenceSet, IEvidenceSet fullSamplingEvidenceSet, Input input) {

        long l1 = System.currentTimeMillis();
        partitionEvidenceSetBuilder = new PartitionEvidenceSetBuilder(predicates, input.getInts());
        this.numberOfPredicates = predicates.getPredicates().size();
        this.input = input;
        this.predicates = predicates;
        this.initSampleEvidence = (HashEvidenceSet) sampleEvidenceSet;
        this.builder = new PartitionEvidenceSetBuilder(predicates, input.getInts());
        this.fullEvidence = (HashEvidenceSet) fullSamplingEvidenceSet;

        // get Initial nodeList by mmcs
        System.out.println(fullSamplingEvidenceSet.size());

        MMCSDC mmcsdc = new MMCSDC(numberOfPredicates, fullSamplingEvidenceSet);

        List<MMCSNode> mmcsNodeList = mmcsdc.getCoverNodes();

        System.out.println(mmcsdc.getCoverNodes().size());

        System.out.println("first mmcs cost: " + (System.currentTimeMillis() - l1));


        // valid nodes list
        List<ClusterPair> startPartition = new ArrayList<>();
        startPartition.add(StrippedPartition.getFullParition(input.getLineCount()));

        l1 = System.currentTimeMillis();
        validNodeList(startPartition, mmcsNodeList, new DenialConstraintSet(), new HashMap<>());
        System.out.println("first valid cost: " + (System.currentTimeMillis() - l1));

        // next cycle
        DenialConstraintSet denialConstraintSet = new DenialConstraintSet();
        l1 = System.currentTimeMillis();
//        triangleOperation(mmcsNodeList, denialConstraintSet);
        singleValidMMCS(mmcsNodeList, denialConstraintSet);
        System.out.println("second mmcs cost: " + (System.currentTimeMillis() - l1));

        // minimize
        l1 = System.currentTimeMillis();
        denialConstraintSet.minimize();
        System.out.println(denialConstraintSet.size());
        System.out.println("minimize cost: " + (System.currentTimeMillis() - l1));
        System.out.println("full evidence set" + newAddedEvidence.size());

    }

    public void singleValidMMCS(List<MMCSNode> mmcsNodeList, DenialConstraintSet denialConstraintSet){
        // valid once and then calculate new evidence for all nodes
        Set<IBitSet> walkedNode = new HashSet<>();
        Set<IBitSet> walkedNode2 = new HashSet<>();
        getLeftEvidenceSet(mmcsNodeList, new DenialConstraintSet());
        DenialConstraintSet denialConstraintSet1 = new DenialConstraintSet();
        HashEvidenceSet newEvi = new HashEvidenceSet();
        for (MMCSNode node : mmcsNodeList) {
            MMCSNode node1 = new MMCSNode(node);
            if (node.completeEvidenceSet != null){
                newEvi.add(node.completeEvidenceSet);
            }

            for (PredicateBitSet iEvidenceSet : newAddedEvidence.getSetOfPredicateSets()) {
                IBitSet coverBy = iEvidenceSet.getBitset().getAnd(node.getElement());
                if (coverBy.cardinality() == 1) {
                    node.crit.get(coverBy.nextSetBit(0)).add(iEvidenceSet);
                }else if (coverBy.cardinality() == 0){
                    node.uncoverEvidenceSet.add(iEvidenceSet);
                }
            }
            for (PredicateBitSet iEvidenceSet: newEvi.getSetOfPredicateSets()){
                IBitSet coverBy = iEvidenceSet.getBitset().getAnd(node1.getElement());
                if (coverBy.cardinality() == 1) {
                    node1.crit.get(coverBy.nextSetBit(0)).add(iEvidenceSet);
                }else if (coverBy.cardinality() == 0){
                    node1.uncoverEvidenceSet.add(iEvidenceSet);
                }
            }


            MMCSDC mmcsdc = new MMCSDC(node, walkedNode);
            mmcsdc.getCoverNodes().forEach(mmcsNode -> denialConstraintSet.add(getDenialConstraint(mmcsNode)));

            MMCSDC mmcsdc1 = new MMCSDC(node1, walkedNode2);
            mmcsdc1.getCoverNodes().forEach(mmcsNode -> denialConstraintSet1.add(getDenialConstraint(mmcsNode)));
//            if (mmcsdc1.getCoverNodes().size() != mmcsdc.getCoverNodes().size()){
//                System.out.println("s");
//            }
        }
        denialConstraintSet1.minimize();
        System.out.println(" dcs1 : " + denialConstraintSet1.size());
    }

    public DenialConstraint getDenialConstraint(MMCSNode node) {
        IBitSet bitSet = node.getElement();
        PredicateBitSet inverse = new PredicateBitSet();
        for (int next = bitSet.nextSetBit(0); next >= 0; next = bitSet.nextSetBit(next + 1)) {
            Predicate predicate = indexProvider.getObject(next); //1
            inverse.add(predicate.getInverse());
        }
        return new DenialConstraint(inverse);
    }

    public void validNodeList(List<ClusterPair> clusterPairs, List<MMCSNode> mmcsNodeList, DenialConstraintSet denialConstraintSet, Map<DenialConstraint, List<MMCSNode>> dcMap2Node) {
        // 1. collect dcs
        mmcsNodeList.forEach(node -> {
            DenialConstraint denialConstraint = getDenialConstraint(node);
            // TODO: because two nodes may get one same dc, so we need map list.
            //  can delete the node and then minimize, get useful dcs and related node
            if (!denialConstraintSet.add(denialConstraint)) {
                dcMap2Node.get(denialConstraint).add(node);
            } else {
                List<MMCSNode> mmcsNodes = new ArrayList<>();
                mmcsNodes.add(node);
                dcMap2Node.put(denialConstraint, mmcsNodes);// 1
            }
        });

        // 2.valid
        Map<DenialConstraint, IEvidenceSet> dcClusterPairMap = new ConcurrentHashMap<>();
//        this.newAddedEvidence = new ResultCompletion(input, predicates).complete( denialConstraintSet, initSampleEvidence, fullEvidence);
        ResultCompletion resultCompletion = new ResultCompletion(input, predicates);

        // TODO: threads
        new ArrayList<>(clusterPairs).forEach(clusterPair -> resultCompletion.completeForHyDC(clusterPair, denialConstraintSet, initSampleEvidence, dcClusterPairMap) );
//        new ResultCompletion(input, predicates).completeForHyDC(clusterPairs, denialConstraintSet, initSampleEvidence, dcClusterPairMap);
        // 3. add left cluster pair to node list
        dcClusterPairMap.forEach((dc, iEvidenceSet) -> {
            dcMap2Node.get(dc).forEach(mmcsNode -> {
                mmcsNode.completeEvidenceSet =  (HashEvidenceSet) iEvidenceSet;
            });
        });

    }

    public void getLeftEvidenceSet(List<MMCSNode> mmcsNodeList, DenialConstraintSet denialConstraintSet){
        mmcsNodeList.forEach(node -> {
            DenialConstraint denialConstraint = getDenialConstraint(node);
            denialConstraintSet.add(denialConstraint);
        });
//        newAddedEvidence =  new ResultCompletion(input, predicates).complete(denialConstraintSet, initSampleEvidence, fullEvidence);
        newAddedEvidence = new ResultCompletion(input, predicates).complete(denialConstraintSet, initSampleEvidence, fullEvidence);
    }

}

