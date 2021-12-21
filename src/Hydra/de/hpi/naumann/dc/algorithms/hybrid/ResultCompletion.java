package Hydra.de.hpi.naumann.dc.algorithms.hybrid;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.PartitionEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.helpers.IndexProvider;
import Hydra.de.hpi.naumann.dc.helpers.SuperSetWalker;
import Hydra.de.hpi.naumann.dc.input.Input;
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
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import utils.TimeCal;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ResultCompletion {

	private Input input;
	private PredicateBuilder predicates;
	
	private static BiFunction<AtomicLongMap<PartitionRefiner>,
	Function<PartitionRefiner, Integer>, Comparator<PartitionRefiner>> resultSorter = (
			selectivityCount, counts) -> (r2, r1) -> {

				long s1 = selectivityCount.get(r1);
				long s2 = selectivityCount.get(r2);

				return Double.compare(1.0d * counts.apply(r1).intValue() / s1, 1.0d * counts.apply(r2).intValue() / s2);

			};;
	
	private static BiFunction<Multiset<PredicatePair> ,
	AtomicLongMap<PartitionRefiner>, Function<PredicatePair, Double>> pairWeight = (
			paircountDC, selectivityCount) -> (pair) -> {
				return Double.valueOf(1.0d * selectivityCount.get(pair) / paircountDC.count(pair));
			};

	public ResultCompletion(Input input, PredicateBuilder predicates) {
		this.input = input;
		this.predicates = predicates;
	}

	/**
	 * 这个方法比较重要
	 * */
	public HashEvidenceSet complete(DenialConstraintSet set, IEvidenceSet sampleEvidence, IEvidenceSet fullEvidence) {
		System.out.println("Checking " + set.size() + " DCs.");

		System.out.println("Building selectivity estimation");//选择性估计计算

		// frequency estimation predicate pairs
		Multiset<PredicatePair> paircountDC = frequencyEstimationForPredicatePairs(set);

		// selectivity estimation for predicates & predicate pairs
		AtomicLongMap<PartitionRefiner> selectivityCount = createSelectivityEstimation(sampleEvidence,
				paircountDC.elementSet());

		ArrayList<PredicatePair> sortedPredicatePairs = getSortedPredicatePairs(paircountDC, selectivityCount);

		IndexProvider<PartitionRefiner> indexProvider = new IndexProvider<>();

		System.out.println("Grouping DCs..");
		Map<IBitSet, List<DenialConstraint>> predicateDCMap = groupDCs(set, sortedPredicatePairs, indexProvider,
				selectivityCount);

		int[] refinerPriorities = getRefinerPriorities(selectivityCount, indexProvider, predicateDCMap);

		SuperSetWalker walker = new SuperSetWalker(predicateDCMap.keySet(), refinerPriorities);

		System.out.println("Calculating partitions..");

		//Evidence Inversion
		HashEvidenceSet resultEv = new HashEvidenceSet();
//		for (PredicateBitSet i : fullEvidence)
//			resultEv.add(i);

		ClusterPair startPartition = StrippedPartition.getFullParition(input.getLineCount());
		int[][] values = input.getInts();
		IEJoin iejoin = new IEJoin(values);
		PartitionEvidenceSetBuilder builder = new PartitionEvidenceSetBuilder(predicates, values);

		long startTime = System.nanoTime();//纳秒
		walker.walk((inter) -> {
			if((System.nanoTime() - startTime) >TimeUnit.MINUTES.toNanos(120))
				return;

			Consumer<ClusterPair> consumer = (clusterPair) -> {
				// this predicateDCMap is mean that this predicate is the last one
//				if (!clusterPair.containsLinePair()){
//					System.out.println("s");
//				}
				List<DenialConstraint> currentDCs = predicateDCMap.get(inter.currentBits);
				if (currentDCs != null) {
					// EtmPoint point = etmMonitor.createPoint("EVIDENCES");
					//
					long l1 = System.currentTimeMillis();
					builder.addEvidences(clusterPair, resultEv);
					TimeCal.add((System.currentTimeMillis() - l1) , 3);
					// point.collect();
				} else {
					inter.nextRefiner.accept(clusterPair);
				}
			};

			//refiner是一系列谓词对
			PartitionRefiner refiner = indexProvider.getObject(inter.newRefiner);
//			System.out.println(refiner);
			ClusterPair partition = inter.clusterPair != null ? inter.clusterPair : startPartition;
			partition.refine(refiner, iejoin, consumer);

		});

		return resultEv;
		// return output;

	}


	private int[] getRefinerPriorities(AtomicLongMap<PartitionRefiner> selectivityCount,
			IndexProvider<PartitionRefiner> indexProvider, Map<IBitSet, List<DenialConstraint>> predicateDCMap) {
		int[] counts2 = new int[indexProvider.size()];
		for (int i = 0; i < counts2.length; ++i) {
			counts2[i] = 1;
		}
		for (IBitSet bitset : predicateDCMap.keySet()) {
			for (int i = bitset.nextSetBit(0); i >= 0; i = bitset.nextSetBit(i + 1)) {
				counts2[i]++;
			}
		}

		ArrayList<PartitionRefiner> refiners = new ArrayList<PartitionRefiner>();

		int[] counts3 = new int[indexProvider.size()];

		for (int i = 0; i < counts3.length; ++i) {
			PartitionRefiner refiner = indexProvider.getObject(i);
			refiners.add(refiner);
		}
		refiners.sort(resultSorter.apply(selectivityCount, refiner -> Integer.valueOf(counts2[indexProvider.getIndex(refiner).intValue()])));

		int i = 0;
		for (PartitionRefiner refiner : refiners) {
			counts3[indexProvider.getIndex(refiner).intValue()] = i;
			++i;
		}
		return counts3;
	}

	private Map<IBitSet, List<DenialConstraint>> groupDCs(DenialConstraintSet set,
			ArrayList<PredicatePair> sortedPredicatePairs, IndexProvider<PartitionRefiner> indexProvider,
			AtomicLongMap<PartitionRefiner> selectivityCount) {
		Map<IBitSet, List<DenialConstraint>> predicateDCMap = new HashMap<>();
		//谓词对映射到整数
		HashMap<PredicatePair, Integer> prios = new HashMap<>();
		for (int i = 0; i < sortedPredicatePairs.size(); ++i) {
			prios.put(sortedPredicatePairs.get(i), Integer.valueOf(i));
		}
		for (DenialConstraint dc : set) {
			Set<PartitionRefiner> refinerSet = getRefinerSet(prios, dc);
			predicateDCMap.computeIfAbsent(indexProvider.getBitSet(refinerSet), (Set) -> new ArrayList<>()).add(dc);
		}
		Set<DenialConstraint> denialConstraints = new HashSet<>();
		for (IBitSet predicates : predicateDCMap.keySet()){
			if ( !denialConstraints.add( predicateDCMap.get(predicates).get(0))){
				System.out.println("s");
			}
		}
		return predicateDCMap;
	}

	private Set<PartitionRefiner> getRefinerSet(HashMap<PredicatePair, Integer> prios, DenialConstraint dc) {
		// a refinement strategy takes as input a predicate and a partition
		// outputs a refined partition that contains only tuple pairs that satisfy the given predicate
		Set<PartitionRefiner> refinerSet = new HashSet<>();

		Set<Predicate> pairSet = new HashSet<>();
		dc.getPredicateSet().forEach(p -> {
			if (StrippedPartition.isSingleSupported(p)) {
				refinerSet.add(p);
			} else {
				pairSet.add(p);
			}
		});
		while (pairSet.size() > 1) {
			PredicatePair bestP = getBest(prios, pairSet);
			refinerSet.add(bestP);
			pairSet.remove(bestP.getP1());
			pairSet.remove(bestP.getP2());
		}
		if (!pairSet.isEmpty()) {
			refinerSet.add(pairSet.iterator().next());
		}
		return refinerSet;
	}

	private PredicatePair getBest(HashMap<PredicatePair, Integer> prios, Set<Predicate> pairSet) {
		int best = -1;
		PredicatePair bestP = null;
		for (Predicate p1 : pairSet) {
			for (Predicate p2 : pairSet) {
				if (p1 != p2) {
					PredicatePair pair = new PredicatePair(p1, p2);
					int score = prios.get(pair).intValue();
					if (score > best) {
						best = score;
						bestP = pair;
					}
				}
			}
		}
		return bestP;
	}

	public static ArrayList<PredicatePair> getSortedPredicatePairs(Multiset<PredicatePair> paircountDC,
			AtomicLongMap<PartitionRefiner> selectivityCount) {
		ArrayList<PredicatePair> sortedPredicatePairs = new ArrayList<>();
		sortedPredicatePairs.addAll(paircountDC.elementSet());
		Function<PredicatePair, Double> weightProv = pairWeight.apply(paircountDC, selectivityCount);
		sortedPredicatePairs.sort(new Comparator<PredicatePair>() {

			@Override
			public int compare(PredicatePair o1, PredicatePair o2) {
				return Double.compare(getPriority(o2), getPriority(o1));
			}

			private double getPriority(PredicatePair o1) {
				return weightProv.apply(o1).doubleValue();
			}
		});
		return sortedPredicatePairs;
	}

	private Multiset<PredicatePair> frequencyEstimationForPredicatePairs(DenialConstraintSet set) {
		Multiset<PredicatePair> paircountDC = HashMultiset.create();
		for (DenialConstraint dc : set) {
			 dc.getPredicateSet().forEach(p1 -> {
				if (StrippedPartition.isPairSupported(p1)) {
					dc.getPredicateSet().forEach(p2 -> {
						if (!p1.equals(p2) && StrippedPartition.isPairSupported(p2)) {
							paircountDC.add(new PredicatePair(p1, p2));
						}
					});
				}
			});
		}
		return paircountDC;
	}


	// 选择性估计
	public static AtomicLongMap<PartitionRefiner> createSelectivityEstimation(IEvidenceSet sampleEvidence,
			Set<PredicatePair> predicatePairs) {
		AtomicLongMap<PartitionRefiner> selectivityCount = AtomicLongMap.create();
		for (PredicateBitSet ps : sampleEvidence) {
			int count = (int) sampleEvidence.getCount(ps);
			ps.forEach(p -> {
				selectivityCount.addAndGet(p, count);
			});
			for (PredicatePair pair : predicatePairs)
				if (pair.bothContainedIn(ps)) {
					selectivityCount.addAndGet(pair, sampleEvidence.getCount(ps));
				}
		}
		return selectivityCount;
	}

	public void completeForHyDC(ClusterPair startPartition, DenialConstraintSet set, IEvidenceSet sampleEvidence, Map<DenialConstraint, IEvidenceSet> dcClusterPairMap) {
//		System.out.println("Checking " + set.size() + " DCs.");
//
//		System.out.println("Building selectivity estimation");//选择性估计计算

		// frequency estimation predicate pairs
		Multiset<PredicatePair> paircountDC = frequencyEstimationForPredicatePairs(set);

		// selectivity estimation for predicates & predicate pairs
		AtomicLongMap<PartitionRefiner> selectivityCount = createSelectivityEstimation(sampleEvidence,
				paircountDC.elementSet());

		ArrayList<PredicatePair> sortedPredicatePairs = getSortedPredicatePairs(paircountDC, selectivityCount);

		IndexProvider<PartitionRefiner> indexProvider = new IndexProvider<>();



//		System.out.println("Grouping DCs..");

		// IBitSet is predicate or predicate pair in dc, predicate ∩ predicate pair = dc
		Map<IBitSet, List<DenialConstraint>> predicateDCMap = groupDCs(set, sortedPredicatePairs, indexProvider,
				selectivityCount);


		int[] refinerPriorities = getRefinerPriorities(selectivityCount, indexProvider, predicateDCMap);

		SuperSetWalker walker = new SuperSetWalker(predicateDCMap.keySet(), refinerPriorities);

		int index = 0;
		for (int i = 0 ; i < indexProvider.size(); ++i){
			if (indexProvider.getObject(i) instanceof Predicate){
				index ++;
			}
		}
//		System.out.println("Calculating partitions..");

		//Evidence Inversion
//		HashEvidenceSet resultEv = new HashEvidenceSet();
//		for (PredicateBitSet i : fullEvidence)
//			resultEv.add(i);

//		ClusterPair startPartition = StrippedPartition.getFullParition(input.getLineCount());
		int[][] values = input.getInts();
		IEJoin iejoin = new IEJoin(values);
		PartitionEvidenceSetBuilder builder = new PartitionEvidenceSetBuilder(predicates, values);

		long startTime = System.nanoTime();//纳秒
		// 1. cal selectivity for all predicate pair and predicate
		// 2. sorted according to selectivity
		// 3. map predicate and predicate pair to dc
		walker.walk((inter) -> {
//			if((System.nanoTime() - startTime) >TimeUnit.MINUTES.toNanos(120))
//				return;

			Consumer<ClusterPair> consumer = (clusterPair) -> {
				List<DenialConstraint> currentDCs = predicateDCMap.get(inter.currentBits);
				if (currentDCs != null) {
//					long l1 = System.currentTimeMillis();
//					builder.addEvidences(clusterPair, resultEv);
//					TimeCal.add((System.currentTimeMillis() - l1) , 3);
					//TODO: this cluster pair is the last result or only for cur predicate or predicate pair?
					HashEvidenceSet hashEvidenceSet = new HashEvidenceSet();
					builder.addEvidences(clusterPair, hashEvidenceSet);
					currentDCs.forEach(denialConstraint -> {
						HashEvidenceSet tmp = (HashEvidenceSet) dcClusterPairMap.getOrDefault(denialConstraint, new HashEvidenceSet());
						tmp.add(hashEvidenceSet);
						dcClusterPairMap.put(denialConstraint, tmp);
					});
				} else {
					inter.nextRefiner.accept(clusterPair);
				}
			};

			//refiner是一系列谓词对
			PartitionRefiner refiner = indexProvider.getObject(inter.newRefiner);
//			System.out.println(refiner);
			ClusterPair partition = inter.clusterPair != null ? inter.clusterPair : startPartition;
			partition.refine(refiner, iejoin, consumer);

		});

//		return resultEv;

	}

}
