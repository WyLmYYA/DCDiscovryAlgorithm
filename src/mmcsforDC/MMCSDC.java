package mmcsforDC;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.ch.javasoft.bitset.search.NTreeSearch;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import utils.TimeCal2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    static IBitSet mask;

    private boolean hasEmptySubset = false;

    public List<MMCSNode> getCoverNodes() {
        return coverNodes;
    }
    NTreeSearch treeSearch = new NTreeSearch();

    public MMCSDC(int numberOfPredicates, IEvidenceSet evidenceSetToCover){

        this.numberOfPredicates = numberOfPredicates;

        candidatePredicates = new LongBitSet(numberOfPredicates);

        mask = new LongBitSet(numberOfPredicates);

        for (int i = 0; i < numberOfPredicates; ++i){
            candidatePredicates.set(i);
            mask.set(i);
        }

        initiate(evidenceSetToCover);

    }


    public void initiate(IEvidenceSet evidenceToCover){

        /**
         *   if there is evidenceSet is empty, return empty DC
        */
        hasEmptySubset = evidenceToCover.getSetOfPredicateSets().stream().anyMatch(predicates -> predicates.getBitset().isEmpty());

        coverNodes = walkDown(new MMCSNode(numberOfPredicates, evidenceToCover));
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
        if(true){
            long l1 = System.currentTimeMillis();
            for (int ne = currentNode.element.nextSetBit(0); ne != -1; ne = currentNode.element.nextSetBit(ne + 1)){
                IBitSet tmp = currentNode.element.clone();
                tmp.set(ne, false);
                for (Predicate p2 : indexProvider.getObject(ne).getInverse().getImplications()){
                    int index = indexProvider.getIndex(p2);
                    if (index < numberOfPredicates){
                        tmp.set(indexProvider.getIndex(p2));
                    }
                }

                if (treeSearch.containsSubset(tmp))return;
            }
            TimeCal2.add((System.currentTimeMillis() - l1), 1);
        }
        if (currentNode.canCover()){
            //TODO: why we should reset: to dynamic usage
            currentNode.resetCandidatePredicates(candidatePredicates);
            treeSearch.add(currentNode.element);
            currentCovers.add(currentNode);
            return;
        }

        /**
         *  chosenEvidence = F ??? cand??? F is next Evidence needs to be covered
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
