package Hydra.de.hpi.naumann.dc.paritions;

import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.input.ParsedColumn;
import Hydra.de.hpi.naumann.dc.predicates.Operator;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.operands.ColumnOperand;
import binaryindextree.BIT;
import binaryindextree.IndexForBIT;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import net.mintern.primitive.Primitive;
import net.mintern.primitive.comparators.IntComparator;
import utils.TimeCal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

public class  IEJoin {
	//inequality join

	public enum Order {
		ASCENDING, DESCENDING
	}

	static int[][] values;

	public IEJoin(Input input) {
		values = input.getInts();
	}

	public IEJoin(int[][] input2s) {
		this.values = input2s;
	}

	public Collection<ClusterPair> calc(ClusterPair clusters, Predicate p1, Predicate p2) {
		Collection<ClusterPair> result = new ArrayList<>();
		calc(clusters, p1, p2, (pair) -> result.add(pair));
		return result;
	}
	public Collection<ClusterPair> calc(ClusterPair clusters, Predicate p1) {
		Collection<ClusterPair> result = new ArrayList<>();
		calc(clusters, p1, (pair) -> result.add(pair));
		return result;
	}

	private int[] getSortedArray(Cluster c, ParsedColumn<?> column, Order order) {
		int[] array = new int[c.size()];
		TIntIterator iter = c.iterator();
		int i = 0;
		/** more 2s*/
		while (iter.hasNext()) {
			array[i++] = iter.next();
		}

		final int cIndex = column.getIndex();

		IntComparator comp = (i1, i2) -> Integer.compare(values[order == Order.DESCENDING ? i2 : i1][cIndex], values[order == Order.DESCENDING ? i1 : i2][cIndex]);
		Primitive.sort(array,comp, false);
		return array;
	}

	private static Order getSortingOrder(int pos, Predicate p) {
		switch (p.getOperator()) {
		case GREATER:
		case GREATER_EQUAL:
			return pos == 0 ? Order.DESCENDING : Order.ASCENDING;
		case LESS:
		case LESS_EQUAL:
			return pos == 0 ? Order.ASCENDING : Order.DESCENDING;
		case EQUAL:
		case UNEQUAL:
		default:
			// WRONG STATE;
			break;
		}
		return null;
	}

	private static Order getSortingOrder( Predicate p) {
		switch (p.getOperator()) {
			case GREATER:
			case GREATER_EQUAL:
				return Order.ASCENDING;
			case LESS:
			case LESS_EQUAL:
				return Order.DESCENDING;
			case EQUAL:
			case UNEQUAL:
			default:
				// WRONG STATE;
				break;
		}
		return null;
	}

	private static int[] getPermutationArray(int[] l2, int[] l1) {
		final int count = l1.length;
		TIntIntMap map = new TIntIntHashMap(count);
		for (int i = 0; i < count; ++i) {
			map.put(l1[i], i);
		}
		int[] result = new int[count];
		for (int i = 0; i < count; ++i) {
			result[i] = map.get(l2[i]);
		}
		return result;
	}

	public static int[] getOffsetArray(int[] l2, int[] l2_, int column1, int column2, boolean c2Rev,
			boolean equal) {
		final int size = l2.length;
		int[] result = new int[size];
		for (int i = 0; i < size; ++i) {
			int value = values[l2[i]][column1];

			result[i] = indexOf(index -> values[l2_[index]][column2], value, l2_.length, c2Rev,
					equal);
		}
		return result;
	}

	public static int getOffset(int l2, int[] l2_, int column1, int column2, boolean c2Rev, boolean equal) {
		int value = values[l2][column1];

		return  indexOf(index -> values[l2_[index]][column2], value, l2_.length, c2Rev,
				equal);

	}

	public static int indexOf2(IntUnaryOperator a, int key, int count, boolean rev, boolean equal) {
		for (int i = 0; i < count; ++i) {
			int c = Integer.compare(key, a.applyAsInt(i));
			if (!rev && equal && c < 0)
				return i;
			if (!rev && !equal && c <= 0)
				return i;
			if (rev && !equal && c >= 0)
				return i;
			if (rev && equal && c > 0)
				return i;
		}
		return count;
	}

	public static int indexOf(IntUnaryOperator a, int key, int count, boolean rev, boolean equal) {
		int lo = 0;
		int hi = count - 1;
		while (lo <= hi) {
			// Key is in a[lo..hi] or not present.
			int mid = lo + (hi - lo) / 2;
			int value = a.applyAsInt(mid);
			int comp = Integer.compare(key, value);
			if (rev) {
				if (!equal && comp >= 0 || equal && comp > 0)
					hi = mid - 1;
				else if (!equal && comp < 0 || equal && comp <= 0)
					lo = mid + 1;
				else
					return mid;
			} else {
				if (!equal && comp <= 0 || equal && comp < 0)
					hi = mid - 1;
				else if (!equal && comp > 0 || equal && comp >= 0)
					lo = mid + 1;
				else
					return mid;
			}

		}
		return lo;
	}


	/**
	 * @Author yoyuan
	 * @Description: BIT for IEJoin optimistic in 2021-11-29
	 * @DateTime: 2021-10-15
	 */
	@SuppressWarnings("rawtypes")
	public void calcForBITJoin(ClusterPair clusters, Predicate p1, Predicate p2, List<ClusterPair> consumer){

		/** Phase1: get init structure */

		long phase1 = System.currentTimeMillis();

		int len1 = clusters.getC1().size();
		int len2 = clusters.getC2().size();




		ParsedColumn<?> columnA = p1.getOperand1().getColumn();
		ParsedColumn<?> columnB = p1.getOperand2().getColumn();
		ParsedColumn<?> columnC =  p2.getOperand1().getColumn();
		ParsedColumn<?> columnD =  p2.getOperand2().getColumn();

		BIT bit = new BIT(len2,columnC, columnD);

		Order order1 = getSortingOrder(p1);
		Order order2 = getSortingOrder(p2);

		bit.order2 = order2;
		bit.p2 = p2;

		int[] L_A = getSortedArray(clusters.getC1(), columnA, order1);
		int[] L_B = getSortedArray(clusters.getC2(), columnB, order1);
		int[] L_D = getSortedArray(clusters.getC2(), columnD, order2);

		int[] O1 = getOffsetArray(L_A, L_B, columnA.getIndex(), columnB.getIndex(), order1 == Order.DESCENDING,
				p1.getOperator() == Operator.GREATER_EQUAL || p1.getOperator() == Operator.LESS_EQUAL);

		HashMap<Integer, Integer> B_D = new HashMap<>();
		for (int i = 0; i < len2; ++i){
			B_D.put(L_B[i], i);
		}
		HashMap<Integer, Integer> A_C = new HashMap<>();
		for (int i = 0; i < len1; ++i){
			A_C.put(L_A[i], i);
		}

		Cluster lastC1 = null;
		Cluster lastC2 = null;


		/** Step1:  init BIT with insert L_D */
		for (int i = 0;i < len2; ++i){
			int D = L_D[i];
			bit.addTuple(B_D.get(D) + 1, D);
		}
		TimeCal.add(System.currentTimeMillis() - phase1, 0);


		IndexForBIT indexForBIT = new IndexForBIT();
		for (int i = 0; i < len1; ++i){
			/** Phase2:  get cluster */
			int A = L_A[i];
//			int offsetForAandB = getOffset(L_B,  L_A[i]);
			int offsetForAandB = O1[i];
			if(offsetForAandB == 0)continue;
//			int C_value = A_C.get(A);
			Cluster c1 = new Cluster(A);
			Cluster c2 = new Cluster();

			long phase2 = System.currentTimeMillis();
			IndexForBIT next = bit.getSum(offsetForAandB, A, c2, indexForBIT);

			if(indexForBIT.equals(next)  && next.hasNext() && lastC1 != null){
				lastC1.add(A);

//				TimeCal.add(System.currentTimeMillis() - phase2, 1);
				continue;
			}else{
				indexForBIT = next;
			}

//			TimeCal.add(System.currentTimeMillis() - phase2, 1);
			long phase3 = System.currentTimeMillis();
			if ( next.hasNext() ){

				TimeCal.add(1, 3);
				/** Phase3: other operation the same as IEJoin */

				ClusterPair pair = new ClusterPair(c1, c2);
				if (pair.containsLinePair()) {
					if (lastC2 != null && c2.equals(lastC2)) {
						lastC1.add(A);
					} else {
						if(lastC1 != null) {
							ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
							consumer.add(pairLast);
						}

						lastC2 = c2;
						lastC1 = c1;
					}
				} else {
					if(lastC1 != null) {
						ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
						consumer.add(pairLast);
					}

					lastC2 = lastC1 = null;
				}

			}else{
				if(lastC1 != null) {
					ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
					consumer.add(pairLast);
				}
				lastC2 = lastC1 = null;
			}
			TimeCal.add(System.currentTimeMillis() - phase3, 2);
		}

		if(lastC1 != null) {
			ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
			consumer.add(pairLast);
		}

	}

	public void calcForTest(ClusterPair clusters, Predicate p1, Predicate p2, List<ClusterPair> Res){

		/** Phase1: get init structure */

		long phase1 = System.currentTimeMillis();

		int len1 = clusters.getC1().size();
		int len2 = clusters.getC2().size();




		ParsedColumn<?> columnA = p1.getOperand1().getColumn();
		ParsedColumn<?> columnB = p1.getOperand2().getColumn();
		ParsedColumn<?> columnC =  p2.getOperand1().getColumn();
		ParsedColumn<?> columnD =  p2.getOperand2().getColumn();

		BIT bit = new BIT(len2,columnC, columnD);

		Order order1 = getSortingOrder(p1);
		Order order2 = getSortingOrder(p2);

		bit.order2 = order2;
		bit.p2 = p2;

		int[] L_A = getSortedArray(clusters.getC1(), columnA, order1);
		int[] L_B = getSortedArray(clusters.getC2(), columnB, order1);
		int[] L_D = getSortedArray(clusters.getC2(), columnD, order2);

		int[] O1 = getOffsetArray(L_A, L_B, columnA.getIndex(), columnB.getIndex(), order1 == Order.DESCENDING,
				p1.getOperator() == Operator.GREATER_EQUAL || p1.getOperator() == Operator.LESS_EQUAL);

		HashMap<Integer, Integer> B_D = new HashMap<>();
		for (int i = 0; i < len2; ++i){
			B_D.put(L_B[i], i);
		}
		HashMap<Integer, Integer> A_C = new HashMap<>();
		for (int i = 0; i < len1; ++i){
			A_C.put(L_A[i], i);
		}

		Cluster lastC1 = null;
		Cluster lastC2 = null;


		/** Step1:  init BIT with insert L_D */
		for (int i = 0;i < len2; ++i){
			int D = L_D[i];
			bit.addTuple(B_D.get(D) + 1, D);
		}
		TimeCal.add(System.currentTimeMillis() - phase1, 0);


		IndexForBIT indexForBIT = new IndexForBIT();
		for (int i = 0; i < len1; ++i){
			/** Phase2:  get cluster */
			int A = L_A[i];
//			int offsetForAandB = getOffset(L_B,  L_A[i]);
			int offsetForAandB = O1[i];
			if(offsetForAandB == 0)continue;
//			int C_value = A_C.get(A);
 			Cluster c1 = new Cluster(A);
			Cluster c2 = new Cluster();

			long phase2 = System.currentTimeMillis();
			IndexForBIT next = bit.getSum(offsetForAandB, A, c2, indexForBIT);

			if(indexForBIT.equals(next)  && next.hasNext() && lastC1 != null){
				lastC1.add(A);

//				TimeCal.add(System.currentTimeMillis() - phase2, 1);
				continue;
			}else{
				indexForBIT = next;
			}

//			TimeCal.add(System.currentTimeMillis() - phase2, 1);
			long phase3 = System.currentTimeMillis();
			if ( next.hasNext() ){

				TimeCal.add(1, 3);
				/** Phase3: other operation the same as IEJoin */

				ClusterPair pair = new ClusterPair(c1, c2);
				if (pair.containsLinePair()) {
					if (lastC2 != null && c2.equals(lastC2)) {
						lastC1.add(A);
					} else {
						if(lastC1 != null) {
							ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
							Res.add(pairLast);
						}

						lastC2 = c2;
						lastC1 = c1;
					}
				} else {
					if(lastC1 != null) {
						ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
						Res.add(pairLast);
					}

					lastC2 = lastC1 = null;
				}

			}else{
				if(lastC1 != null) {
					ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
					Res.add(pairLast);
				}
				lastC2 = lastC1 = null;
			}
			TimeCal.add(System.currentTimeMillis() - phase3, 2);
		}

		if(lastC1 != null) {
			ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
			Res.add(pairLast);
		}

	}
	/**
	 * @Author yoyuan
	 * @Description: BIT for IEJoin
	 * @DateTime: 2021-10-15
	 */
	@SuppressWarnings("rawtypes")
	public void calc3(ClusterPair clusters, Predicate p1, Predicate p2, Consumer<ClusterPair> consumer){

		/** Phase1: get init structure */

		long phase1 = System.currentTimeMillis();

		int len1 = clusters.getC1().size();
		int len2 = clusters.getC2().size();




		ParsedColumn<?> columnA = p1.getOperand1().getColumn();
		ParsedColumn<?> columnB = p1.getOperand2().getColumn();
		ParsedColumn<?> columnD =  p2.getOperand2().getColumn();
		BIT bit = new BIT(len2,columnA,columnD);
		Order order1 = getSortingOrder(p1);
		Order order2 = getSortingOrder(p2);

		bit.order2 = order2;
		int[] L_A = getSortedArray(clusters.getC1(), columnA, order1);
		int[] L_B = getSortedArray(clusters.getC2(), columnB, order1);
		int[] L_D = getSortedArray(clusters.getC2(), columnD, order2);


		int[] O1 = getOffsetArray(L_A, L_B, columnA.getIndex(), columnB.getIndex(), order1 == Order.DESCENDING,
				p1.getOperator() == Operator.GREATER || p1.getOperator() == Operator.LESS);

		HashMap<Integer, Integer> B_D = new HashMap<>();
		for (int i = 0; i < len2; ++i){
			B_D.put(L_B[i], i);
		}
		HashMap<Integer, Integer> A_C = new HashMap<>();
		for (int i = 0; i < len1; ++i){
			A_C.put(L_A[i], i);
		}

		Cluster lastC1 = null;
		Cluster lastC2 = null;


		/** Step1:  init BIT with insert L_D */
		for (int i = 0;i < len2; ++i){
			int D = L_D[i];
			bit.addTuple(B_D.get(D) + 1, D);
		}
		TimeCal.add(System.currentTimeMillis() - phase1, 0);


		for (int i = 0; i < len1; ++i){
			/** Phase2:  get cluster */
			long phase2 = System.currentTimeMillis();
			int A = L_A[i];
//			int offsetForAandB = getOffset(L_B,  L_A[i]);
			int offsetForAandB = O1[i];

			List<Cluster> A_BNodesCluster = bit.getSum(offsetForAandB);

			Cluster c1 = new Cluster(A);
			Cluster c2 = new Cluster();

			int C_value = A_C.get(A);
			if ( A_BNodesCluster.size() != 0){
				A_BNodesCluster.forEach(cluster -> {
					int[] arr = cluster.toArray();
					int index = getOffset(arr, C_value), len = arr.length;
					for(int i2 = index; i2 < len; ++i2){
						c2.add(arr[i2]);
					}

				});
				TimeCal.add(System.currentTimeMillis() - phase2, 1);

				/** Phase3: other operation the same as IEJoin */
				long phase3 = System.currentTimeMillis();
				ClusterPair pair = new ClusterPair(c1, c2);
				if (pair.containsLinePair()) {
					if (lastC2 != null && c2.equals(lastC2)) {
						lastC1.add(A);
					} else {
						if(lastC1 != null) {
							ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
							consumer.accept(pairLast);
						}

						lastC2 = c2;
						lastC1 = c1;
					}
				} else {
					if(lastC1 != null) {
						ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
						consumer.accept(pairLast);
					}

					lastC2 = lastC1 = null;
				}
				TimeCal.add(System.currentTimeMillis() - phase3, 2);
			}else{
				if(lastC1 != null) {
					ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
					consumer.accept(pairLast);
				}
				lastC2 = lastC1 = null;
			}
		}

		if(lastC1 != null) {
			ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
			consumer.accept(pairLast);
		}

	}


	/**
	 * @Description: binary search
	 * @Author yoyuan
	 * @DateTime: 2021-10-16 before dawn
	 * */
	private int getOffset(int[] arr, double value) {
		if(arr[arr.length - 1] < value) return arr.length;
		// get first one bigger than value
		int min =0;
		int max = arr.length-1;
		while(min<max){
			int mid = (min+max)>>>1;
			int midValue = arr[mid];
			if(midValue>value){
				max = mid;
			}else if(arr[mid]<=value){
				min = mid+1;
			}
		}
		return min;
	}

	/**
	 *   IEjoin for predicates pair
	 */
	@SuppressWarnings("rawtypes")
	public void calc(ClusterPair clusters, Predicate p1, Predicate p2, Consumer<ClusterPair> consumer) {
		/** Phase1: get init structure */
		long phase1 = System.currentTimeMillis();
		ColumnOperand op11 = p1.getOperand1();
		ParsedColumn<?> columnX = op11.getColumn();
		ColumnOperand op12 = p1.getOperand2();
		ParsedColumn<?> columnX_ = op12.getColumn();
		ColumnOperand op21 = p2.getOperand1();
		ParsedColumn<?> columnY = op21.getColumn();
		ColumnOperand op22 = p2.getOperand2();
		ParsedColumn<?> columnY_ = op22.getColumn();

		Order order1 = getSortingOrder(0, p1);
		Order order2 = getSortingOrder(1, p2);

//		EtmPoint pointS = etmMonitor.createPoint("sortings..");
		/** sort 55s*/
		int[] L1 = getSortedArray(clusters.getC1(), columnX, order1);
		int[] L1_ = getSortedArray(clusters.getC2(), columnX_, order1);
		int[] L2 = getSortedArray(clusters.getC1(), columnY, order2);
		int[] L2_ = getSortedArray(clusters.getC2(), columnY_, order2);
//		pointS.collect();




		/** permutation array is map the index for L2 tuple in L1 30s*/
//		EtmPoint pointP = etmMonitor.createPoint("permutations..");
		int[] P = getPermutationArray(L2, L1);
		int[] P_ = getPermutationArray(L2_, L1_);
//		pointP.collect();



		/** O1 O2 is the index of the first one not bigger than the value in permutation for L1, L2 respectively
		 *  6s
		 * */
//		EtmPoint pointO = etmMonitor.createPoint("offsets..");
		int[] O1 = getOffsetArray(L1, L1_, columnX.getIndex(), columnX_.getIndex(), order1 == Order.DESCENDING,
				p1.getOperator() == Operator.GREATER || p1.getOperator() == Operator.LESS);
		int[] O2 = getOffsetArray(L2, L2_, columnY.getIndex(), columnY_.getIndex(), order2 == Order.DESCENDING,
				p2.getOperator() == Operator.GREATER_EQUAL || p2.getOperator() == Operator.LESS_EQUAL);
//		pointO.collect();


		/** the bit-array B*/
		LongBitSet bitset = new LongBitSet(clusters.getC2().size());

//		EtmPoint pointJ = etmMonitor.createPoint("Join");
		Cluster lastC1 = null;
		Cluster lastC2 = null;


		TimeCal.add(System.currentTimeMillis() - phase1, 0);
		/** begin with scanning L2  here can replace with clusters.getC2().size()*/
		for (int i = 0; i < clusters.getC1().size(); ++i) {
			// relative position of r_i in L2'
			/** the first bigger off2 in L2' than L2[i]*/
			/** Phase2:  get cluster */
			long phase2 = System.currentTimeMillis();
			int off2 = O2[i];


			/** 0 - O2[i-1] has been scanned by 0 - i-1, so this time we don't need to set 0 - i-1
			 *  the index in bitset value with 1 represent, the tuple with the same index in L2' satisfies p2 with L2[I]
			 * */
			int start = i > 0 ? O2[i - 1] : 0;
			for (int j = start; j < off2; ++j) {
				bitset.set(P_[j]);
			}

			/** P[i] is the index of L2[i] in L1[i], so the O1[P[i]] is the first bigger value in L1' than L2[i]'s value in column columnX*/
			int start2 = O1[P[i]];

			if (lastC2 != null && start >= off2 && bitset.nextSetBit(start2) == bitset.nextSetBit(O1[P[i - 1]])) {
				lastC1.add(L2[i]);
				continue;
			}

			Cluster c1 = new Cluster(L2[i]);

			/** get satisfied tuple pair with two predicates */
			int count = 0;

			/** transform bitset*/

			for (int k = bitset.nextSetBit(start2); k >= 0; k = bitset.nextSetBit(k + 1))
				++count;




			if (count > 0) {
				Cluster c2 = new Cluster(new TIntArrayList(count));

				// Tax10k 10s
				for (int k = bitset.nextSetBit(start2); k >= 0; k = bitset.nextSetBit(k + 1))
					c2.add(L1_[k]);


				TimeCal.add(System.currentTimeMillis() - phase2, 1);
				/** Phase3: other operation the same as IEJoin */
				long phase3 = System.currentTimeMillis();
				ClusterPair pair = new ClusterPair(c1, c2);
				if (pair.containsLinePair()) {
					if (lastC2 != null && c2.equals(lastC2)) {
						lastC1.add(L2[i]);
					} else {
						if(lastC1 != null) {
							ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
							consumer.accept(pairLast);
						}
						
						lastC2 = c2;
						lastC1 = c1;
					}
				} else {
					if(lastC1 != null) {
						ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
						consumer.accept(pairLast);
					}
					
					lastC2 = lastC1 = null;
				}

				TimeCal.add(System.currentTimeMillis() - phase3, 2);
			} else {
				if(lastC1 != null) {
					ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
					consumer.accept(pairLast);
				}
				lastC2 = lastC1 = null;
			}
		}
		if(lastC1 != null) {
			ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
			consumer.accept(pairLast);
		}
//		pointJ.collect();
	}

	public void calc2ForTest(ClusterPair clusters, Predicate p1, Predicate p2, List<ClusterPair> res) {
		System.out.println(p1);
		System.out.println(p2);
		/** Phase1: get init structure */
		long phase1 = System.currentTimeMillis();
		ColumnOperand op11 = p1.getOperand1();
		ParsedColumn<?> columnX = op11.getColumn();
		ColumnOperand op12 = p1.getOperand2();
		ParsedColumn<?> columnX_ = op12.getColumn();
		ColumnOperand op21 = p2.getOperand1();
		ParsedColumn<?> columnY = op21.getColumn();
		ColumnOperand op22 = p2.getOperand2();
		ParsedColumn<?> columnY_ = op22.getColumn();

		Order order1 = getSortingOrder(0, p1);
		Order order2 = getSortingOrder(1, p2);

//		EtmPoint pointS = etmMonitor.createPoint("sortings..");
		/** sort 55s*/
		int[] L1 = getSortedArray(clusters.getC1(), columnX, order1);
		int[] L1_ = getSortedArray(clusters.getC2(), columnX_, order1);
		int[] L2 = getSortedArray(clusters.getC1(), columnY, order2);
		int[] L2_ = getSortedArray(clusters.getC2(), columnY_, order2);
//		pointS.collect();




		/** permutation array is map the index for L2 tuple in L1 30s*/
//		EtmPoint pointP = etmMonitor.createPoint("permutations..");
		int[] P = getPermutationArray(L2, L1);
		int[] P_ = getPermutationArray(L2_, L1_);
//		pointP.collect();



		/** O1 O2 is the index of the first one not bigger than the value in permutation for L1, L2 respectively
		 *  6s
		 * */
//		EtmPoint pointO = etmMonitor.createPoint("offsets..");
		int[] O1 = getOffsetArray(L1, L1_, columnX.getIndex(), columnX_.getIndex(), order1 == Order.DESCENDING,
				p1.getOperator() == Operator.GREATER || p1.getOperator() == Operator.LESS);
		int[] O2 = getOffsetArray(L2, L2_, columnY.getIndex(), columnY_.getIndex(), order2 == Order.DESCENDING,
				p2.getOperator() == Operator.GREATER_EQUAL || p2.getOperator() == Operator.LESS_EQUAL);
//		pointO.collect();


		/** the bit-array B*/
		LongBitSet bitset = new LongBitSet(clusters.getC2().size());

//		EtmPoint pointJ = etmMonitor.createPoint("Join");
		Cluster lastC1 = null;
		Cluster lastC2 = null;


		TimeCal.add(System.currentTimeMillis() - phase1, 0);
		/** begin with scanning L2  here can replace with clusters.getC2().size()*/
		for (int i = 0; i < clusters.getC1().size(); ++i) {
			// relative position of r_i in L2'
			/** the first bigger off2 in L2' than L2[i]*/
			/** Phase2:  get cluster */
			long phase2 = System.currentTimeMillis();
			int off2 = O2[i];


			/** 0 - O2[i-1] has been scanned by 0 - i-1, so this time we don't need to set 0 - i-1
			 *  the index in bitset value with 1 represent, the tuple with the same index in L2' satisfies p2 with L2[I]
			 * */
			int start = i > 0 ? O2[i - 1] : 0;
			for (int j = start; j < off2; ++j) {
				bitset.set(P_[j]);
			}

			/** P[i] is the index of L2[i] in L1[i], so the O1[P[i]] is the first bigger value in L1' than L2[i]'s value in column columnX*/
			int start2 = O1[P[i]];

			if (lastC2 != null && start >= off2 && bitset.nextSetBit(start2) == bitset.nextSetBit(O1[P[i - 1]])) {
				lastC1.add(L2[i]);

				continue;
			}

			Cluster c1 = new Cluster(L2[i]);

			/** get satisfied tuple pair with two predicates */
			int count = 0;

			/** transform bitset*/

			for (int k = bitset.nextSetBit(start2); k >= 0; k = bitset.nextSetBit(k + 1))
				++count;




			if (count > 0) {
				TimeCal.add(1, 3);
				Cluster c2 = new Cluster(new TIntArrayList(count));

				// Tax10k 10s
				long timeForAddRes = System.currentTimeMillis();
				for (int k = bitset.nextSetBit(start2); k >= 0; k = bitset.nextSetBit(k + 1))
					c2.add(L1_[k]);
				TimeCal.add(System.currentTimeMillis() - timeForAddRes, 4);

				TimeCal.add(System.currentTimeMillis() - phase2, 1);
				/** Phase3: other operation the same as IEJoin */
				long phase3 = System.currentTimeMillis();
				ClusterPair pair = new ClusterPair(c1, c2);
				if (pair.containsLinePair()) {
					if (lastC2 != null && c2.equals(lastC2)) {
						lastC1.add(L2[i]);
					} else {
						if(lastC1 != null) {
							ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
							res.add(pairLast);
						}

						lastC2 = c2;
						lastC1 = c1;
					}
				} else {
					if(lastC1 != null) {
						ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
						res.add(pairLast);
					}

					lastC2 = lastC1 = null;
				}

				TimeCal.add(System.currentTimeMillis() - phase3, 2);
			} else {
				if(lastC1 != null) {
					ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
					res.add(pairLast);
				}
				lastC2 = lastC1 = null;
			}
		}
		if(lastC1 != null) {
			ClusterPair pairLast = new ClusterPair(lastC1, lastC2);
			res.add(pairLast);
		}
//		pointJ.collect();
	}

	/**
	 * @Description: for single predicates  IEjoin
	 * this way is Inequality join, not the best way described in Hydra paper
	*/
	public void calc(ClusterPair clusters, Predicate p1, Consumer<ClusterPair> consumer) {

		/** get operator and column  */
		ColumnOperand<?> op11 = p1.getOperand1();
		ParsedColumn<?> columnX = op11.getColumn();
		ColumnOperand<?> op12 = p1.getOperand2();
		ParsedColumn<?> columnX_ = op12.getColumn();

		Order order1 = getSortingOrder(0, p1);


		/** sort with two columns */
		int[] L1 = getSortedArray(clusters.getC1(), columnX, order1);
		int[] L1_ = getSortedArray(clusters.getC2(), columnX_, order1);

		
		int start2 = 0;

		/** begin algorithm with scanning L1 */
		for (int i = 0; i < L1.length; ++i) {
			/** find the first tuple which can satisfy the predicate with tuple i */
			while (start2 < L1_.length && !p1.satisfies(L1[i], L1_[start2]))
				++start2;
			if (start2 == L1_.length)
				break;

			/** get cluster in which all tuples satisfy the predicate with the first tuple
			 *  this is once a tuple satisfies, all tuples before satisfy
			 * */
			Cluster c1 = new Cluster(L1[i]);
			while (i + 1 < L1.length && p1.satisfies(L1[i + 1], L1_[start2])) {
				++i;
				c1.add(L1[i]);
			}

			Cluster c2 = new Cluster();
			for (int j = start2; j < L1_.length; ++j) {
				c2.add(L1_[j]);
			}
			ClusterPair pair = new ClusterPair(c1, c2);
			if (pair.containsLinePair()) {
				consumer.accept(pair);
			}
		}
	}

	// use List for HyDC to collect result
	public void calc(ClusterPair clusters, Predicate p1, List<ClusterPair> consumer) {

		/** get operator and column  */
		ColumnOperand<?> op11 = p1.getOperand1();
		ParsedColumn<?> columnX = op11.getColumn();
		ColumnOperand<?> op12 = p1.getOperand2();
		ParsedColumn<?> columnX_ = op12.getColumn();

		Order order1 = getSortingOrder(0, p1);


		/** sort with two columns */
		int[] L1 = getSortedArray(clusters.getC1(), columnX, order1);
		int[] L1_ = getSortedArray(clusters.getC2(), columnX_, order1);


		int start2 = 0;

		/** begin algorithm with scanning L1 */
		for (int i = 0; i < L1.length; ++i) {
			/** find the first tuple which can satisfy the predicate with tuple i */
			while (start2 < L1_.length && !p1.satisfies(L1[i], L1_[start2]))
				++start2;
			if (start2 == L1_.length)
				break;

			/** get cluster in which all tuples satisfy the predicate with the first tuple
			 *  this is once a tuple satisfies, all tuples before satisfy
			 * */
			Cluster c1 = new Cluster(L1[i]);
			while (i + 1 < L1.length && p1.satisfies(L1[i + 1], L1_[start2])) {
				++i;
				c1.add(L1[i]);
			}

			Cluster c2 = new Cluster();
			for (int j = start2; j < L1_.length; ++j) {
				c2.add(L1_[j]);
			}
			ClusterPair pair = new ClusterPair(c1, c2);
			if (pair.containsLinePair()) {
				consumer.add(pair);
			}
		}
	}
}
