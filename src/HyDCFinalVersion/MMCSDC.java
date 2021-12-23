package HyDCFinalVersion;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.de.hpi.naumann.dc.algorithms.hybrid.ResultCompletion;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.PartitionEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.paritions.IEJoin;
import Hydra.de.hpi.naumann.dc.paritions.StrippedPartition;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet.indexProvider;

/**
 * @Author yoyuan
 * @Description:  we treat mmcs process as a deep traversal of a tree,
 *                IEvidenceSet is evidence set to cover whose element is PredicateBitSet, a set of predicates = an evidence
 *
 * @DateTime: 2021/9/26 14:47
 */

public class MMCSDC {

    /**
     *  in DC, this is number of predicates, generally this is the attribute for FD or vertices for Hypergraph
    */
    private int numberOfPredicates;

    /**
     *  each node represent current a minimal cover set, here, is a valid DC
    */
    private List<MMCSNode> coverNodes = new ArrayList<>();

    /**
     *  `cand` for MMCS, with the transversal walkDown, this will live update
    */
    static IBitSet candidatePredicates;

    private boolean hasEmptySubset = false;

    public List<MMCSNode> getCoverNodes() {
        return coverNodes;
    }

    /**
     * params added for HyDC
     */
    private final PredicateBuilder predicates;

    private final Input input;

    public static IEJoin ieJoin;

    public HashEvidenceSet all = new HashEvidenceSet();

    public MMCSDC(int numberOfPredicates, IEvidenceSet evidenceSetToCover, PredicateBuilder predicates, Input input){

        this.numberOfPredicates = numberOfPredicates;
        ieJoin = new IEJoin(input.getInts());
        candidatePredicates = new LongBitSet(numberOfPredicates);

        this.predicates = predicates;
        this.input = input;

        for (int i = 0; i < numberOfPredicates; ++i){
            candidatePredicates.set(i);
        }

        PartitionEvidenceSetBuilder partitionEvidenceSetBuilder = new PartitionEvidenceSetBuilder(predicates, input.getInts());
        partitionEvidenceSetBuilder.addEvidences(StrippedPartition.getFullParition(input.getLineCount()), all );

        initiate(evidenceSetToCover);

    }


    public void initiate(IEvidenceSet evidenceToCover){

        /**
         *   if there is evidenceSet is empty, return empty DC
        */
        hasEmptySubset = evidenceToCover.getSetOfPredicateSets().stream().anyMatch(predicates -> predicates.getBitset().isEmpty());

        coverNodes = walkDown(new MMCSNode(numberOfPredicates, evidenceToCover, input.getLineCount()));
    }

    /**
     *  root is the root of the deep transversal tree, the elements is null, and the uncover is full EvidenceSet
    */
    List<MMCSNode>  walkDown(MMCSNode root){

        List<MMCSNode> currentCovers = new ArrayList<>();

        walkDown(root, currentCovers);

        return currentCovers;
    }


    public  void walkDown(MMCSNode currentNode, List<MMCSNode> currentCovers){
        if (currentNode.canCover()){
//            for (int next = currentNode.element.nextSetBit(0); next >= 0; next = currentNode.element.nextSetBit(next + 1)){
//                System.out.println(indexProvider.getObject(next));
//            }
//                currentCovers.add(currentNode);
//            return;
//        }
            // we need to valid current partial dc is valid dc or not

            //  check is there any predicate needed combination not be refined, and update cluster pair
            boolean cover = true;
            for (PredicateBitSet setOfPredicateSet : all.getSetOfPredicateSets()) {
                if (setOfPredicateSet.getBitset().getAnd(currentNode.element).cardinality() == 0){
                    cover = false;
                    break;
                }
            }

            if (currentNode.lastNeedCombinationPredicate != null && currentNode.clusterPairs.size() != 0){
                currentNode.refinePS(currentNode.lastNeedCombinationPredicate, ieJoin);
            }
            if (currentNode.isValidResult()){
                currentCovers.add(currentNode);
            }else{
                // not a valid result means cluster pair not empty, we need get added evidence set
                // after this func, uncover update, and is a complete evidence for currNode, so cluster pair will be null
//                if (cover){
//                    System.out.println("s");
//                }
                if (cover){
                    System.out.println("s");
                }
                currentNode.getAddedEvidenceSet(predicates, input);
                if (currentNode.uncoverEvidenceSet.size() == 0)
                    currentCovers.add(currentNode);
                else{
                    // get Result, this step clusterPair is empty, so we can get valid result
                    int before = currentCovers.size();
                    walkDown(currentNode, currentCovers);
//                    System.out.println("add covers: " + (currentCovers.size() - before));
//                    HashEvidenceSet tmp = new HashEvidenceSet();
//                    tmp.add(currentNode.uncoverEvidenceSet);
//                    currentNode.uncoverEvidenceSet.add(all);
//
//                    List<MMCSNode> res = new ArrayList<>();
//                    walkDown(currentNode, res);
//                    System.out.println(res.size());
//                    if (res.size() != (currentCovers.size() - before)){
//                        System.out.println("s");
//                    }
//                    currentNode.uncoverEvidenceSet = tmp;

                }

            }
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

            MMCSNode childNode = currentNode.getChildNode(next, prunedCandidate);


            if(childNode.isGlobalMinimal()){
                walkDown(childNode, currentCovers);
                currentNode.uncoverEvidenceSet.add(childNode.addEvidences);
                currentNode.addEvidences.add(childNode.addEvidences);
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
