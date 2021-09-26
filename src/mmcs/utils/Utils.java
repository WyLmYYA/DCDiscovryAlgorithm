package mmcs.utils;

import java.util.BitSet;
import java.util.Comparator;

public class Utils {

    /**
     * Compare two BitSets from the lowest bit
     */
    public static Comparator<BitSet> BitsetComparator() {
        return (a, b) -> {
            if (a.equals(b)) return 0;

            BitSet xor = (BitSet) a.clone();
            xor.xor(b);
            int lowestDiff = xor.nextSetBit(0);

            if (lowestDiff == -1) return 0;
            return b.get(lowestDiff) ? 1 : -1;
        };
    }


}
