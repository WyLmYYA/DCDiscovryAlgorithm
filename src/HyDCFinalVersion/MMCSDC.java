package HyDCFinalVersion;

import HyDC.MMCSHyDCNode;
import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.ch.javasoft.bitset.search.NTreeSearch;
import Hydra.de.hpi.naumann.dc.algorithms.hybrid.ResultCompletion;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.PartitionEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.helpers.IndexProvider;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.IEJoin;
import Hydra.de.hpi.naumann.dc.paritions.LinePair;
import Hydra.de.hpi.naumann.dc.paritions.StrippedPartition;
import Hydra.de.hpi.naumann.dc.predicates.Operator;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.Closure;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import utils.TimeCal2;

import java.util.*;

import static Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet.indexProvider;

/**
 * @Author yoyuan
 * @Description:  we treat mmcs process as a deep traversal of a tree,
 *                IEvidenceSet is evidence set to cover whose element is PredicateBitSet, a set of predicates = an evidence
 *
 * @DateTime: 2021/9/26 14:47
 */

public class MMCSDC {

    private final static boolean ENABLE_TRANSITIVE_CHECK = true;
    /**
     *  in DC, this is number of predicates, generally this is the attribute for FD or vertices for Hypergraph
    */
    private int numberOfPredicates;

    /**
     *  each node represent current a minimal cover set, here, is a valid DC
    */
    private List<MMCSNode> coverNodes = new ArrayList<>();

    public DenialConstraintSet denialConstraintSet = new DenialConstraintSet();

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
    public static PredicateBuilder predicates;

    public static Input input;

    public static IEJoin ieJoin;

    NTreeSearch treeSearch = new NTreeSearch();

    public static int noAddedCount = 0;

    public static Set<LinePair> calculatedPair = new HashSet<>();

    Map<PredicateBitSet, DenialConstraintSet.MinimalDCCandidate> constraintsClosureMap = new HashMap<>();


    public MMCSDC(int numberOfPredicates, IEvidenceSet evidenceSetToCover, PredicateBuilder predicates, Input input){

        this.numberOfPredicates = numberOfPredicates;
        ieJoin = new IEJoin(input.getInts());
        candidatePredicates = new LongBitSet(numberOfPredicates);

        this.predicates = predicates;
        this.input = input;

        for (int i = 0; i < numberOfPredicates; ++i){
            candidatePredicates.set(i);
        }


        initiate(evidenceSetToCover);

    }


    public void initiate(IEvidenceSet evidenceToCover){

        /**
         *   if there is evidenceSet is empty, return empty DC
        */
        hasEmptySubset = evidenceToCover.getSetOfPredicateSets().stream().anyMatch(predicates -> predicates.getBitset().isEmpty());

        walkDown(new MMCSNode(numberOfPredicates, evidenceToCover, input.getLineCount()));
    }

    /**
     *  root is the root of the deep transversal tree, the elements is null, and the uncover is full EvidenceSet
    */
//    void walkDown(MMCSNode root){
//
//        List<MMCSNode> currentCovers = new ArrayList<>();
//
//        walkDown(root);
//    }


    public  void walkDown(MMCSNode currentNode){

        // minimize in mmcs,
        if(ENABLE_TRANSITIVE_CHECK){
            long l1 = System.currentTimeMillis();
            for (int ne = currentNode.element.nextSetBit(0); ne != -1; ne = currentNode.element.nextSetBit(ne + 1)){
                IBitSet tmp = currentNode.element.clone();
                tmp.set(ne, false);
                for (Predicate p2 : indexProvider.getObject(ne).getInverse().getImplications()){
                    tmp.set(indexProvider.getIndex(p2));
                }

                if (treeSearch.containsSubset(tmp))return;
            }
            TimeCal2.add((System.currentTimeMillis() - l1), 1);
        }
        if (currentNode.canCover()){
            // we need to valid current partial dc is valid dc or not

            //  check is there any predicate needed combination not be refined, and update cluster pair

            if (currentNode.needRefine) {
                currentNode.refine();
            }

            if (currentNode.lastNeedCombinationPredicate != null && currentNode.clusterPairs.size() != 0){
                currentNode.refinePS(currentNode.lastNeedCombinationPredicate, ieJoin);
            }


            if (currentNode.isValidResult()){

                treeSearch.add(currentNode.element);
                DenialConstraint dc = currentNode.getDenialConstraint();
                denialConstraintSet.add(dc);

            }else{
                // not a valid result means cluster pair not empty, we need get added evidence set
                // after this func, uncover update, and is a complete evidence for currNode, so cluster pair will be null

                currentNode.getAddedEvidenceSet();
                walkDown(currentNode);
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
                walkDown(childNode);

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

        // Triviality:
        predicates.forEach(predicate1 -> {
            int redundantIndex = indexProvider.getIndex(predicate1);
            if (redundantIndex < numberOfPredicates)
                tmp.set(indexProvider.getIndex(predicate1), false);
        });

        // Implication:
        predicate.getImplications().forEach(predicate1 -> {
            int redundantIndex = indexProvider.getIndex(predicate1);
            if (redundantIndex < numberOfPredicates)
                tmp.set(indexProvider.getIndex(predicate1), false);
        });

        return tmp;
    }

}
