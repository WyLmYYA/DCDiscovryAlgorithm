package binaryindextree;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IndexForBIT {

    public List<Integer> NodeIndex;

    public List<Integer> offsetInNode;

    public IndexForBIT(List<Integer> nodeIndex, List<Integer> offsetInNode) {
        NodeIndex = nodeIndex;
        this.offsetInNode = offsetInNode;
    }

    public IndexForBIT(){
        NodeIndex = new ArrayList<>();
        offsetInNode = new ArrayList<>();
    }

    public boolean hasNext(){
        return offsetInNode.size() != 0 && NodeIndex.size() != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexForBIT that = (IndexForBIT) o;
        return Objects.equals(NodeIndex, that.NodeIndex) && Objects.equals(offsetInNode, that.offsetInNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(NodeIndex, offsetInNode);
    }
}
