package HyDCNoSort;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.de.hpi.naumann.dc.algorithms.hybrid.Hydra;
import Hydra.de.hpi.naumann.dc.algorithms.hybrid.ResultCompletion;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.PartitionEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.SystematicLinearEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.helpers.IndexProvider;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.IEJoin;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import gnu.trove.list.array.TIntArrayList;
import mmcsforDC.MMCSNode;

import java.util.*;

import static Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet.indexProvider;

/**
 * @Author yoyuan
 * @Description:  we treat mmcs process as a deep traversal of a tree,
 *                IEvidenceSet is evidence set to cover whose element is PredicateBitSet, a set of predicates = an evidence
 *
 * @DateTime: 2021/9/26 14:47
 */

public class MMCSHyDC {

    /**
     *  in DC, this is number of predicates, generally this is the attribute for FD or vertices for Hypergraph
    */
    private final int numberOfPredicates;

    /**
     *  each node represent current a minimal cover set, here, is a valid DC
    */
    private List<MMCSHyDCNode> coverNodes = new ArrayList<>();

    /**
     *  `cand` for MMCS, with the transversal walkDown, this will live update
    */
    static IBitSet candidatePredicates;


    private final PredicateBuilder predicates;

    private final Input input;

    public IEvidenceSet sampleEvidenceSet;

    public List<MMCSHyDCNode> getCoverNodes() {
        return coverNodes;
    }

    private HashEvidenceSet fullEvidenceSet = new HashEvidenceSet();



    public MMCSHyDC(int numOfNeedCombinePredicate, Input input, PredicateBuilder predicates,HashEvidenceSet sampleEvidenceSet, HashEvidenceSet evidenceSetToCover){

        this.numberOfPredicates = predicates.getPredicates().size();

        candidatePredicates = new LongBitSet(numberOfPredicates);

        this.sampleEvidenceSet = sampleEvidenceSet;

        this.predicates = predicates;

        for (int i = 0; i < numberOfPredicates; ++i){
            candidatePredicates.set(i);
        }
        this.input = input;

        TIntArrayList c = new TIntArrayList();
        for(int i = 0; i < input.getLineCount(); ++i){
            c.add(i);
        }
        new PartitionEvidenceSetBuilder(predicates, input.getInts()).addEvidences(new ClusterPair(new Cluster(c)), fullEvidenceSet);


        initiate(evidenceSetToCover, numOfNeedCombinePredicate);



    }


    public void initiate(HashEvidenceSet evidenceToCover, int numOfNeedCombinePredicate){

        /**
         *   if there is evidenceSet is empty, return empty DC
        */
        boolean hasEmptySubset = evidenceToCover.getSetOfPredicateSets().stream().anyMatch(predicates -> predicates.getBitset().isEmpty());
        if (hasEmptySubset)return;


        coverNodes = walkDown(new MMCSHyDCNode(input.getLineCount(), numberOfPredicates, evidenceToCover,numOfNeedCombinePredicate), new HashSet<>());
    }

    /**
     *  root is the root of the deep transversal tree, the elements is null, and the uncover is full EvidenceSet
    */
    List<MMCSHyDCNode>  walkDown(MMCSHyDCNode root, Set<IBitSet> walkedNodes){

        // record the result
        List<MMCSHyDCNode> currentCovers = new ArrayList<>();

        walkDown(root, currentCovers,walkedNodes);

        return currentCovers;
    }


    public HashEvidenceSet walkDown(MMCSHyDCNode currentNode, List<MMCSHyDCNode> currentCovers, Set<IBitSet> walkedNodes){
        // canCover only means current uncover is empty
//        if (!walkedNodes.add(currentNode.getElement()))return new HashEvidenceSet();

        if (currentNode.canCover()){

            // TODO: 目前bit join只支持大于小于，还没加大于等于以及小于等于，先用IEJoin
            // if get a partial dc, we need to complete it
            HashEvidenceSet curAddedEvidenceSet = new ResultCompletion(input, predicates).complete(new DenialConstraintSet(currentNode.getDenialConstraint()), sampleEvidenceSet, new HashEvidenceSet());
            if (curAddedEvidenceSet.size() != 0){
                currentNode.uncoverEvidenceSet.add(curAddedEvidenceSet);
                currentNode.newEvidenceSet.add(curAddedEvidenceSet);
                currentNode.completeEvidenceSet.add(curAddedEvidenceSet);
//                walkDownToResult(currentNode, currentCovers, walkedNodes);
            }else {
//                fullEvidenceSet.getSetOfPredicateSets().forEach(predicates1 -> {
//                    if (predicates1.getBitset().getAnd(currentNode.getElement()).cardinality() == 0){
//                        System.out.println("s");
//                    }
//                });
                currentCovers.add(currentNode);
                return currentNode.newEvidenceSet;
            }



        }

        /**
         *  chosenEvidence = F ∩ cand， F is next Evidence needs to be covered
        */
        PredicateBitSet nextPredicates =  currentNode.getNextEvidence();

        IBitSet chosenEvidence = currentNode.candidatePredicates.getAnd(nextPredicates.getBitset());


        /**
         *  cand = cand \ C, we don't change the candidatePredicates of current node
        */

        IBitSet nextCandidatePredicates = currentNode.candidatePredicates.getAndNot(chosenEvidence);


        /**
         * try every CandidatePredicates to add, and walkDown
        */
        for (int next = chosenEvidence.nextSetBit(0); next >= 0; next = chosenEvidence.nextSetBit(next + 1)){

            /** get Trivial prune */
            IBitSet prunedCandidate = PruneNextPredicates(nextCandidatePredicates,next);

            MMCSHyDCNode childNode = currentNode.getChildNode(next, prunedCandidate);

            if(childNode != null){
                HashEvidenceSet newEvidenceSet = walkDown(childNode, currentCovers, walkedNodes);
                currentNode.completeEvidenceSet.add(newEvidenceSet);
                currentNode.newEvidenceSet.add(newEvidenceSet);
                currentNode.uncoverEvidenceSet.add(newEvidenceSet);
                nextCandidatePredicates.set(next);
            }

        }
        return currentNode.newEvidenceSet;

    }

    public  void walkDownToResult(MMCSHyDCNode currentNode, List<MMCSHyDCNode> currentCovers, Set<IBitSet> walkedNodes){
        if (!walkedNodes.add(currentNode.getElement()))return;
        if (currentNode.canCover()){
//            HashEvidenceSet curAddedEvidenceSet = new ResultCompletion(input, predicates).complete(new DenialConstraintSet(currentNode.getDenialConstraint()), sampleEvidenceSet);
//            if(curAddedEvidenceSet.size() != 0){
//                System.out.println("s");
//            }
//            fullEvidenceSet.getSetOfPredicateSets().forEach(predicates1 -> {
//                if (predicates1.getBitset().getAnd(currentNode.getElement()).cardinality() == 0){
//                    System.out.println("s");
//                }
//            });
            currentCovers.add(currentNode);
            return;
        }

        /**
         *  chosenEvidence = F ∩ cand， F is next Evidence needs to be covered
         */
        PredicateBitSet nextPredicates =  currentNode.getNextEvidence();

        IBitSet chosenEvidence = currentNode.candidatePredicates.getAnd(nextPredicates.getBitset());


        /**
         *  cand = cand \ C, we don't change the candidatePredicates of current node
         */

        IBitSet nextCandidatePredicates = currentNode.candidatePredicates.getAndNot(chosenEvidence);


        /**
         * try every CandidatePredicates to add, and walkDown
         */
        for (int next = chosenEvidence.nextSetBit(0); next >= 0; next = chosenEvidence.nextSetBit(next + 1)){

            /** get Trivial prune */
            IBitSet prunedCandidate = PruneNextPredicates(nextCandidatePredicates,next);

            MMCSHyDCNode childNode = currentNode.getChildNode(next, prunedCandidate);
            if(childNode != null) {
                walkDownToResult(childNode, currentCovers,walkedNodes);
                nextCandidatePredicates.set(next);
            }
        }



    }


    private IBitSet PruneNextPredicates(IBitSet nextCandidatePredicates, int next) {
        IBitSet tmp = nextCandidatePredicates.clone();

        Predicate predicate = indexProvider.getObject(next);

        Collection<Predicate> predicates =  predicate.getRedundants();

        predicates.forEach(predicate1 -> {
            int redundantIndex = indexProvider.getIndex(predicate1);
            if (redundantIndex < numberOfPredicates)
                tmp.set(indexProvider.getIndex(predicate1), false);
        });
        return tmp;
    }




}
