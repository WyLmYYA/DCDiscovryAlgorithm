package mmcs.algorithm;

import java.util.BitSet;
import java.util.stream.IntStream;

public class Subset {

    public BitSet elements;

    public Subset(BitSet _elements) {
        elements = (BitSet) _elements.clone();
    }

    public Subset(Subset sb) {
        elements = (BitSet) sb.elements.clone();
    }

    public boolean hasElement(int e) {
        return elements.get(e);
    }

    public int getCoverCount(BitSet coverElements) {
        BitSet intersection = (BitSet) coverElements.clone();
        intersection.and(elements);
        return intersection.cardinality();
    }

    /**
     * @return -1 if this subset has no critical cover w.r.t coverEle
     */
    public int getCritCover(BitSet coverEle) {
        BitSet intersec = (BitSet) coverEle.clone();
        intersec.and(elements);
        return intersec.cardinality() == 1 ? intersec.nextSetBit(0) : -1;
    }

    public IntStream getEleStream() {
        return elements.stream();
    }

    void clear(int e) {
        elements.clear(e);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Subset && ((Subset) obj).elements.equals(elements);
    }


}
