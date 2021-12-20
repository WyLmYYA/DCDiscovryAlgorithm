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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet.indexProvider;

/**
 * @Author yoyuan
 * @Description:
 * @DateTime: 2021/12/18 23:15
 */
public class HyDC {
    SystematicLinearEvidenceSetBuilder systematicLinearEvidenceSetBuilder;

    int numberOfPredicates;

    Input input;

    PredicateBuilder predicates;

    HashEvidenceSet initSampleEvidence;

    HashEvidenceSet newAddedEvidence = new HashEvidenceSet();

    PartitionEvidenceSetBuilder builder;

    public void run(PredicateBuilder predicates, IEvidenceSet sampleEvidenceSet, IEvidenceSet fullSamplingEvidenceSet, Input input) {

        systematicLinearEvidenceSetBuilder = new SystematicLinearEvidenceSetBuilder(predicates, 0);
//        HashEvidenceSet newEvidence = systematicLinearEvidenceSetBuilder.getEvidenceSet(StrippedPartition.getFullParition(input.getLineCount()));
        this.numberOfPredicates = predicates.getPredicates().size();
        this.input = input;
        this.predicates = predicates;
        this.initSampleEvidence = (HashEvidenceSet) sampleEvidenceSet;
        this.builder = new PartitionEvidenceSetBuilder(predicates, input.getInts());

        // get Initial nodeList
        System.out.println(fullSamplingEvidenceSet.size());

        MMCSDC mmcsdc = new MMCSDC(numberOfPredicates, fullSamplingEvidenceSet);

        List<MMCSNode> mmcsNodeList = mmcsdc.getCoverNodes();

        System.out.println(mmcsdc.getCoverNodes().size());

        List<ClusterPair> startPartition = new ArrayList<>();
        startPartition.add(StrippedPartition.getFullParition(input.getLineCount()));

        validNodeList(startPartition, mmcsNodeList, new DenialConstraintSet(), new ConcurrentHashMap<>());

        // 4. iterator for every node
        DenialConstraintSet denialConstraintSet = new DenialConstraintSet();
        triangleOperation(mmcsNodeList, denialConstraintSet);
//        singleValidMMCS(mmcsNodeList, denialConstraintSet);
        denialConstraintSet.minimize();
        System.out.println(denialConstraintSet.size());
        System.out.println("full evidence set" + ( newAddedEvidence.size() + fullSamplingEvidenceSet.size()));

    }

    public HashEvidenceSet triangleOperation(List<MMCSNode> mmcsNodeList, DenialConstraintSet denialConstraintSet) {

        HashEvidenceSet newEvidence = new HashEvidenceSet();
        Set<IBitSet> walkedNode = new HashSet<>();
        for (MMCSNode mmcsNode : mmcsNodeList) {
            // TODO: {16, 16} : {16} can return immediately
            if (mmcsNode.clusterPairs == null || mmcsNode.clusterPairs.size() == 0){
                denialConstraintSet.add(getDenialConstraint(mmcsNode));

            }else {
                boolean hasNext = true;

                // optimistic1: we can choose calculate cluster pair or valid, this can be set a threshold may not be 100
                int count = mmcsNode.clusterPairs.stream().mapToInt(clusterPair -> (int) clusterPair.getLinePairCount()).sum();

                if (newEvidence.size() == 0 || count < 100) {
                    newEvidence.add(systematicLinearEvidenceSetBuilder.getEvidenceSet(mmcsNode.clusterPairs));
                    hasNext = false;
                }

                if (newEvidence.size() == 0){
                    denialConstraintSet.add(getDenialConstraint(mmcsNode));
                    continue;
                }

                for (PredicateBitSet iEvidenceSet : newEvidence.getSetOfPredicateSets()) {
                    IBitSet coverBy = iEvidenceSet.getBitset().getAnd(mmcsNode.getElement());
                    if (coverBy.cardinality() == 1) {
                        mmcsNode.crit.get(coverBy.nextSetBit(0)).add(iEvidenceSet);
                    } else if (coverBy.cardinality() == 0) {
                        mmcsNode.uncoverEvidenceSet.add(iEvidenceSet);
                    }
                }
                MMCSDC mmcsdc = new MMCSDC(mmcsNode, walkedNode);
                if (hasNext){
                    validNodeList(mmcsNode.clusterPairs, mmcsdc.getCoverNodes(),new DenialConstraintSet(), new ConcurrentHashMap<>());
                    newEvidence.add(triangleOperation(mmcsdc.getCoverNodes(), denialConstraintSet));
                }else {
                    mmcsdc.getCoverNodes().forEach(node -> denialConstraintSet.add(getDenialConstraint(node)));
                }

            }
        }
        return newEvidence;

    }

    public void singleValidMMCS(List<MMCSNode> mmcsNodeList, DenialConstraintSet denialConstraintSet){
        // valid once and then calculate new evidence for all nodes
        Set<IBitSet> walkedNode = new HashSet<>();
        mmcsNodeList.forEach(node -> {
            if (node.clusterPairs == null || node.clusterPairs.size() == 0) {
                denialConstraintSet.add(getDenialConstraint(node));
            }else{
                HashEvidenceSet newEvidence = systematicLinearEvidenceSetBuilder.getEvidenceSet(node.clusterPairs);
                newAddedEvidence.add(newEvidence);
                if (newEvidence.size() == 0)denialConstraintSet.add(getDenialConstraint(node));
                else {
                    for (PredicateBitSet iEvidenceSet : newAddedEvidence.getSetOfPredicateSets()) {
                        IBitSet coverBy = iEvidenceSet.getBitset().getAnd(node.getElement());
                        if (coverBy.cardinality() == 1) {
                            node.crit.get(coverBy.nextSetBit(0)).add(iEvidenceSet);
                        }else if (coverBy.cardinality() == 0){
                            node.uncoverEvidenceSet.add(iEvidenceSet);
                        }
                    }
                    MMCSDC mmcsdc = new MMCSDC(node, walkedNode);
                    mmcsdc.getCoverNodes().forEach(mmcsNode -> denialConstraintSet.add(getDenialConstraint(mmcsNode)));
                }
            }
        });
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
        Map<DenialConstraint, List<ClusterPair>> dcClusterPairMap = new ConcurrentHashMap<>();
        ResultCompletion resultCompletion = new ResultCompletion(input, predicates);
        new ArrayList<>(clusterPairs).forEach(clusterPair -> resultCompletion.completeForHyDC(clusterPair, denialConstraintSet, initSampleEvidence, dcClusterPairMap));
//        new ResultCompletion(input, predicates).completeForHyDC(clusterPairs, denialConstraintSet, initSampleEvidence, dcClusterPairMap);
        // 3. add left cluster pair to node list
        dcClusterPairMap.forEach((dc, clusterPairs1) -> {
            dcMap2Node.get(dc).forEach(mmcsNode -> {
                mmcsNode.clusterPairs = new ArrayList<>(clusterPairs1);
            });
        });
    }

}
