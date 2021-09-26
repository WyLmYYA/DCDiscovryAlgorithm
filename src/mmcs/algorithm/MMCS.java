package mmcs.algorithm;


import mmcs.utils.Utils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * original MMCS algorithm for discovering minimal hitting sets
 */
public class MMCS {

    /**
     * number of elements or attributes
     */
    private int nElements;

    /**
     * each node represents a minimal cover set
     */
    private List<MMCSNode> coverNodes = new ArrayList<>();

    /**
     * true iff there's an empty subset to cover (which could never be covered).
     * return no cover set if true but walk down without the empty subset
     */
    private boolean hasEmptySubset = false;


    public MMCS(int nEle) {
        nElements = nEle;
    }

    /**
     * @param bitSetsToCover unique BitSets representing Subsets to be covered
     */
    public void initiate(List<BitSet> bitSetsToCover) {
        hasEmptySubset = bitSetsToCover.stream().anyMatch(BitSet::isEmpty);

        List<Subset> subsets = bitSetsToCover.stream().filter(bs -> !bs.isEmpty()).map(Subset::new).collect(Collectors.toList());

        MMCSNode initNode = new MMCSNode(nElements, subsets);

        walkDown(initNode, coverNodes);
    }

    /**
     * down from nd on the search tree, find all minimal hitting sets
     */
    void walkDown(MMCSNode nd, List<MMCSNode> newNodes) {
        if (nd.isCover()) {
            newNodes.add(nd);
            return;
        }

        // configure cand for child nodes
        BitSet childCand = nd.getCand();
        BitSet addCandidates = nd.getAddCandidates();
        childCand.andNot(addCandidates);

        addCandidates.stream().forEach(e -> {
            MMCSNode childNode = nd.getChildNode(e, childCand);
            if (childNode.isGlobalMinimal()) {
                walkDown(childNode, newNodes);
                childCand.set(e);
            }
        });
    }


    public List<BitSet> getMinCoverSets() {
        return hasEmptySubset ? new ArrayList<>() : coverNodes.stream()
                .map(MMCSNode::getElements)
                .sorted(Utils.BitsetComparator())
                .collect(Collectors.toList());
    }


}
