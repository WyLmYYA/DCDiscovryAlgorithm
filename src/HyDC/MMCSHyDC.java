package HyDC;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.SystematicLinearEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.helpers.IndexProvider;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.IEJoin;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;

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
    private int numberOfPredicates;

    /**
     *  each node represent current a minimal cover set, here, is a valid DC
    */
    private List<MMCSHyDCNode> coverNodes = new ArrayList<>();

    /**
     *  `cand` for MMCS, with the transversal walkDown, this will live update
    */
    static IBitSet candidatePredicates;

    static IBitSet mask;

    private boolean hasEmptySubset = false;

    private PredicateBuilder predicates;

    private IEJoin iEjoin;

    private Input input;

    private SystematicLinearEvidenceSetBuilder systematicLinearEvidenceSetBuilder;

    public List<MMCSHyDCNode> getCoverNodes() {
        return coverNodes;
    }

    public static IndexProvider<Predicate> indexForPredicate;

    public MMCSHyDC(int numOfNeedCombinePredicate, Input input, PredicateBuilder predicates, HashEvidenceSet evidenceSetToCover){

        this.numberOfPredicates = predicates.getPredicates().size();

        candidatePredicates = new LongBitSet(numberOfPredicates);

        this.predicates = predicates;

        iEjoin = new IEJoin(input);

        for (int i = 0; i < numberOfPredicates; ++i){
            candidatePredicates.set(i);
        }
        this.input = input;
        systematicLinearEvidenceSetBuilder = new SystematicLinearEvidenceSetBuilder(predicates, 0);



        initiate(evidenceSetToCover, numOfNeedCombinePredicate);

    }


    public void initiate(HashEvidenceSet evidenceToCover, int numOfNeedCombinePredicate){

        /**
         *   if there is evidenceSet is empty, return empty DC
        */
        hasEmptySubset = evidenceToCover.getSetOfPredicateSets().stream().anyMatch(predicates -> predicates.getBitset().isEmpty());
        if (hasEmptySubset)return;

        coverNodes = walkDown(new MMCSHyDCNode(input.getLineCount(), numberOfPredicates, evidenceToCover,numOfNeedCombinePredicate));
    }

    /**
     *  root is the root of the deep transversal tree, the elements is null, and the uncover is full EvidenceSet
    */
    List<MMCSHyDCNode>  walkDown(MMCSHyDCNode root){

        // record the result
        List<MMCSHyDCNode> currentCovers = new ArrayList<>();

        walkDown(root, currentCovers);

        return currentCovers;
    }


    public HashEvidenceSet walkDown(MMCSHyDCNode currentNode, List<MMCSHyDCNode> currentCovers){
        // canCover only means current uncover is empty
        if (currentNode.canCover()){
            if (currentNode.isNeedCombine && !currentNode.isCombineWithParent){
                List<ClusterPair> newResult = new ArrayList<>();
                for (ClusterPair clusterPair : currentNode.clusterPairs) {
                    clusterPair.refine(currentNode.lastPredicate.getInverse(), iEjoin, newResult::add);
                }
                currentNode.clusterPairs = newResult;
                currentNode.isCombineWithParent = true;
            }
            //so we need to judge if there are other evidences that sampling doesn't get
            if (currentNode.clusterPairs.size() == 0) {
                currentCovers.add(currentNode);
                return new HashEvidenceSet();
            }
            else {
                // update evidence in node， uncoverEvidenceSet presents the last set to cover,
                // we use newEvidenceSet maintain all new evidences that we need to back to parents
                for (ClusterPair clusterPair : currentNode.clusterPairs){
                    HashEvidenceSet evidenceSet = systematicLinearEvidenceSetBuilder.getEvidenceSet(clusterPair, currentNode);
                    Set<PredicateBitSet> complete = currentNode.completeEvidenceSet.getSetOfPredicateSets();
                    evidenceSet.forEach(evidence -> {
                        if (complete.add(evidence)){
                            currentNode.newEvidenceSet.add(evidence);
                            currentNode.uncoverEvidenceSet.add(evidence);
                        }
                    });

                }
                // TODO: ClusterPair: {12} -> {12,12}
                if (currentNode.uncoverEvidenceSet.size() == 0){
                    currentCovers.add(currentNode);
                    return new HashEvidenceSet();
                }

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
            List<ClusterPair> clusterPairs = new ArrayList<>(currentNode.clusterPairs);

            IBitSet prunedCandidate = PruneNextPredicates(nextCandidatePredicates,next);

            MMCSHyDCNode childNode = currentNode.getChildNode(next, prunedCandidate, iEjoin);


            if(childNode != null){
                HashEvidenceSet newEvidenceSet = walkDown(childNode, currentCovers);
                currentNode.completeEvidenceSet.add(newEvidenceSet);
                currentNode.newEvidenceSet.add(newEvidenceSet);
                currentNode.uncoverEvidenceSet.add(newEvidenceSet);
                nextCandidatePredicates.set(next);
            }
            currentNode.clusterPairs = clusterPairs;

        }
        return currentNode.newEvidenceSet;

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
