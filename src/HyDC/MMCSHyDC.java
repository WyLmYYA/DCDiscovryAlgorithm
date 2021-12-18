package HyDC;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.de.hpi.naumann.dc.algorithms.hybrid.ResultCompletion;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.TroveEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.SystematicLinearEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.helpers.IndexProvider;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.IEJoin;
import Hydra.de.hpi.naumann.dc.paritions.StrippedPartition;
import Hydra.de.hpi.naumann.dc.predicates.PartitionRefiner;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.PredicatePair;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.util.concurrent.AtomicLongMap;
import gnu.trove.list.array.TIntArrayList;

import java.util.*;

import static Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet.indexProvider;

/**
 * @Author yoyuan
 * @Description: we treat mmcs process as a deep traversal of a tree,
 * IEvidenceSet is evidence set to cover whose element is PredicateBitSet, a set of predicates = an evidence
 * @DateTime: 2021/9/26 14:47
 */

public class MMCSHyDC {

    /**
     * in DC, this is number of predicates, generally this is the attribute for FD or vertices for Hypergraph
     */
    private int numberOfPredicates;

    /**
     * each node represent current a minimal cover set, here, is a valid DC
     */
    private List<MMCSHyDCNode> coverNodes = new ArrayList<>();

    /**
     * `cand` for MMCS, with the transversal walkDown, this will live update
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

    public static Map<ArrayList<PartitionRefiner>, ClusterPair> clusterPairMap = new HashMap<>();

    public static ClusterPairTreeNode treeRoot;

    public final HashEvidenceSet sampleEvidence;

    public static AtomicLongMap<PartitionRefiner> selectivity;

    public MMCSHyDC(Input input, PredicateBuilder predicates, HashEvidenceSet evidenceSetToCover) {

        this.numberOfPredicates = predicates.getPredicates().size();

        candidatePredicates = new LongBitSet(numberOfPredicates);

        this.predicates = predicates;

        iEjoin = new IEJoin(input);

        for (int i = 0; i < numberOfPredicates; ++i) {
            candidatePredicates.set(i);
        }
        this.input = input;

        systematicLinearEvidenceSetBuilder = new SystematicLinearEvidenceSetBuilder(predicates, 0);

        selectivity = AtomicLongMap.create();

        initiate(evidenceSetToCover);

        sampleEvidence = evidenceSetToCover;

        treeRoot = new ClusterPairTreeNode();
    }


    public void initiate(HashEvidenceSet evidenceToCover) {

        /**
         *   if there is evidenceSet is empty, return empty DC
         */
        hasEmptySubset = evidenceToCover.getSetOfPredicateSets().stream().anyMatch(predicates -> predicates.getBitset().isEmpty());
        if (hasEmptySubset) return;

        coverNodes = walkDown(new MMCSHyDCNode(input.getLineCount(), numberOfPredicates, evidenceToCover));
    }

    /**
     * root is the root of the deep transversal tree, the elements is null, and the uncover is full EvidenceSet
     */
    List<MMCSHyDCNode> walkDown(MMCSHyDCNode root) {

        // record the result
        List<MMCSHyDCNode> currentCovers = new ArrayList<>();

        walkDown(root, currentCovers);

        return currentCovers;
    }


    public HashEvidenceSet walkDown(MMCSHyDCNode currentNode, List<MMCSHyDCNode> currentCovers) {
        // canCover only me
        // ans current uncover is empty
        if (currentNode.canCover()) {

            List<ClusterPair> clusterPairs = refine(currentNode);
            if (clusterPairs.size() == 0) {
                currentCovers.add(currentNode);
                return currentNode.newEvidenceSet;
            } else {
                for (ClusterPair clusterPair : clusterPairs) {
                    HashEvidenceSet evidenceSet = systematicLinearEvidenceSetBuilder.getEvidenceSet(clusterPair);
                    Set<PredicateBitSet> complete = currentNode.completeEvidenceSet.getSetOfPredicateSets();
                    evidenceSet.forEach(evidence -> {
                        if (complete.add(evidence)) {
                            currentNode.newEvidenceSet.add(evidence);
                            currentNode.uncoverEvidenceSet.add(evidence);
                        }
                    });
                }
                if (currentNode.uncoverEvidenceSet.size() == 0) {
                    currentCovers.add(currentNode);
                    return new HashEvidenceSet();
                }
            }
        }

        /**
         *  chosenEvidence = F ∩ cand， F is next Evidence needs to be covered
         */
        PredicateBitSet nextPredicates = currentNode.getNextEvidence();

        IBitSet chosenEvidence = currentNode.candidatePredicates.getAnd(nextPredicates.getBitset());


        /**
         *  cand = cand \ C, we don't change the candidatePredicates of current node
         */

        IBitSet nextCandidatePredicates = currentNode.candidatePredicates.getAndNot(chosenEvidence);


        /**
         * try every CandidatePredicates to add, and walkDown
         */
        for (int next = chosenEvidence.nextSetBit(0); next >= 0; next = chosenEvidence.nextSetBit(next + 1)) {

            /** get Trivial prune */

            IBitSet prunedCandidate = PruneNextPredicates(nextCandidatePredicates, next);

            MMCSHyDCNode childNode = currentNode.getChildNode(next, prunedCandidate, iEjoin);

            if (childNode != null) {
                HashEvidenceSet newEvidenceSet = walkDown(childNode, currentCovers);
                currentNode.completeEvidenceSet.add(newEvidenceSet);
                currentNode.newEvidenceSet.add(newEvidenceSet);
                currentNode.uncoverEvidenceSet.add(newEvidenceSet);
                nextCandidatePredicates.set(next);
            }

        }
        return currentNode.newEvidenceSet;

    }

    private IBitSet PruneNextPredicates(IBitSet nextCandidatePredicates, int next) {
        IBitSet tmp = nextCandidatePredicates.clone();

        Predicate predicate = indexProvider.getObject(next);

        Collection<Predicate> predicates = predicate.getRedundants();

        predicates.forEach(predicate1 -> {
            int redundantIndex = indexProvider.getIndex(predicate1);
            if (redundantIndex < numberOfPredicates)
                tmp.set(indexProvider.getIndex(predicate1), false);
        });
        return tmp;
    }

    public List<ClusterPair> refine(MMCSHyDCNode currentNode) {
        Multiset<PredicatePair> pairCount = HashMultiset.create();

        Multiset<PartitionRefiner> preCount = HashMultiset.create();

        // 1. get predicate pair
        currentNode.predicatesForElement.forEach(pre1 -> {
            if (StrippedPartition.isPairSupported(pre1)) {
                currentNode.predicatesForElement.forEach(pre2 -> {
                    if (!pre1.equals(pre2) && StrippedPartition.isPairSupported(pre2)) {
                        pairCount.add(new PredicatePair(pre1, pre2));
                        preCount.add(new PredicatePair(pre1, pre2));
                    }
                });
            } else {
                preCount.add(pre1);
            }
        });

        // 2. calculate predicate pair and predicate selectivity
        createPairSelectivityEstimation(sampleEvidence, pairCount.elementSet());

        // combine single and pair, and sort them together
        ArrayList<PartitionRefiner> sortedPredicate = getSortedPredicate(preCount);

        // 3. find is there calculated clusterPair
        ClusterPairTreeNode node = treeRoot;
        List<ClusterPair> tmp = null;
        int idx = 0;
        // find the last node for single predicate
        while (node.children.size() > 0 && idx < sortedPredicate.size()) {
            PartitionRefiner predicate = sortedPredicate.get(idx);
            boolean isContinue = false;
            for (int j = 0; j < node.children.size(); ++j) {
                if (node.children.get(j).partitionRefiner.equals(predicate)) {
                    tmp = node.clusterPair;
                    node = node.children.get(j);
                    ++idx;
                    isContinue = true;
                    break;
                }
            }
            if (!isContinue) break;
        }
        List<ClusterPair> clusterPairs;
        if (tmp == null) {
            TIntArrayList c = new TIntArrayList();
            for (int i = 0; i < input.getLineCount(); ++i) {
                c.add(i);
            }
            clusterPairs = new ArrayList<>();
            clusterPairs.add(new ClusterPair(new Cluster(c)));
        } else clusterPairs = new ArrayList<>(tmp);

        //4. cal the remain predicate and pair
        for (int i = idx; i < sortedPredicate.size(); ++i) {
            PartitionRefiner refiner = sortedPredicate.get(i);
            ClusterPairTreeNode clusterPairTreeNode = new ClusterPairTreeNode(refiner);
            List<ClusterPair> newResult = new ArrayList<>();
            // TODO: 目前只支持大于小于，还没debug加大于等于以及小于等于
            if (refiner instanceof Predicate) {
                for (ClusterPair clusterPair : clusterPairs) {
                    clusterPair.refinePsPublic(((Predicate) refiner).getInverse(), iEjoin, newResult);
                }
            } else {
                for (ClusterPair clusterPair : clusterPairs) {
                    iEjoin.calcForTest(clusterPair, refiner, newResult);
                }
            }
            clusterPairTreeNode.clusterPair = newResult;
            node.children.add(clusterPairTreeNode);
            node = clusterPairTreeNode;
        }

        return node.clusterPair;
    }

    public void createPairSelectivityEstimation(IEvidenceSet sampleEvidence, Set<PredicatePair> predicatePairs) {
        for (PredicateBitSet ps : sampleEvidence) {
            int count = (int) sampleEvidence.getCount(ps);
            // add if contain to avoid duplicate calculate
            ps.forEach(p -> {
                if (!selectivity.containsKey(p)) selectivity.addAndGet(p, count);
            });
            for (PredicatePair pair : predicatePairs)
                if (pair.bothContainedIn(ps) && !selectivity.containsKey(pair)) {
                    selectivity.addAndGet(pair, sampleEvidence.getCount(ps));
                }
        }
    }

    public ArrayList<PartitionRefiner> getSortedPredicate(Multiset<PartitionRefiner> predicates) {
        ArrayList<PartitionRefiner> arrayList = new ArrayList<>();
        arrayList.addAll(predicates);
        Collections.sort(arrayList, (o1, o2) -> (int) (selectivity.get(o2) - selectivity.get(o1)));
        return arrayList;
    }



}
