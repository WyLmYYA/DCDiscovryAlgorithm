package Hydra.de.hpi.naumann.dc.evidenceset.build;

import HyDCFinalVersion.MMCSDC;
import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.TroveEvidenceSet;
import Hydra.de.hpi.naumann.dc.input.ColumnPair;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.LinePair;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateSetFactory;
import test.RunHydra;
import utils.TimeCal2;

import java.util.*;
import java.util.function.Function;

public class PartitionEvidenceSetBuilder extends EvidenceSetBuilder {
	private Collection<ColumnPair> pairs;
	private int[][] input2s;

	public PartitionEvidenceSetBuilder(PredicateBuilder predicates2, int[][] values) {
		super(predicates2);
		pairs = predicates.getColumnPairs();
		createSets(pairs);
		this.input2s = values;
	}




	public void addEvidences(ClusterPair clusterPair, IEvidenceSet evidenceSet, Set<String> calP) {

		//4950

		PredicateBitSet staticSet = null;
		int lastI = -1;
		// transform clusterPair  to line pair

		Iterator<LinePair> iter = clusterPair.getLinePairIterator();


		while (iter.hasNext()) {
			LinePair lPair = iter.next();

			int i = lPair.getLine1();
			int j = lPair.getLine2();
			if (i != j) {
				int[] row1 = input2s[i];
				if (staticSet == null || i != lastI)
					staticSet = getStatic(pairs, row1);

				int[] row2 = input2s[j];
				PredicateBitSet set = getPredicateSet(staticSet, pairs, row1, row2);
				evidenceSet.add(set);
				PredicateBitSet set2 = getPredicateSet(getStatic(pairs, row2), pairs, row2, row1);
				evidenceSet.add(set2);

				//349766

			}

			lastI = i;

		}

	}

	static Set<LinePair> calP = new HashSet<>();

	static Map<LinePair, PredicateBitSet> mapEvidence = new HashMap<>();
	public void addEvidencesForHyDC(ClusterPair clusterPair, IEvidenceSet evidenceSet) {
		PredicateBitSet staticSet = null;
		int lastI = -1;
		// transform clusterPair  to line pair
		Iterator<LinePair> iter = clusterPair.getLinePairIterator();
		while (iter.hasNext()) {
			LinePair lPair = iter.next();
			int i = lPair.getLine1();
			int j = lPair.getLine2();
			if (i != j) {
				int[] row1 = input2s[i];
				if (staticSet == null || i != lastI)
					staticSet = getStatic(pairs, row1);

				int[] row2 = input2s[j];

				if (mapEvidence.containsKey(lPair)){
					evidenceSet.add(mapEvidence.get(lPair));
				}else{
					PredicateBitSet set = getPredicateSet(staticSet, pairs, row1, row2);
					evidenceSet.add(set);
					mapEvidence.put(lPair, set);
				}

				if (mapEvidence.containsKey(lPair.getInverse())){
					evidenceSet.add(mapEvidence.get(lPair.getInverse()));
				}else{
					PredicateBitSet set1 = getPredicateSet(staticSet, pairs, row2, row1);
					evidenceSet.add(set1);
					mapEvidence.put(lPair, set1);
				}
			}

			lastI = i;

		}

	}
	public void addEvidences(ClusterPair clusterPair, IEvidenceSet evidenceSet) {
		PredicateBitSet staticSet = null;
		int lastI = -1;
		// transform clusterPair  to line pair
		Iterator<LinePair> iter = clusterPair.getLinePairIterator();
		while (iter.hasNext()) {
			LinePair lPair = iter.next();
//			if (!calP.add(lPair)){
//				continue;
//			}
			int i = lPair.getLine1();
			int j = lPair.getLine2();
			if (i != j) {
				int[] row1 = input2s[i];
				if (staticSet == null || i != lastI)
					staticSet = getStatic(pairs, row1);

				int[] row2 = input2s[j];

				PredicateBitSet set = getPredicateSet(staticSet, pairs, row1, row2);
				evidenceSet.add(set);

				PredicateBitSet set1 = getPredicateSet(staticSet, pairs, row2, row1);
				evidenceSet.add(set1);
				//349766
			}

			lastI = i;

		}

	}
	public void addEvidences(Iterator<LinePair> iter, IEvidenceSet evidenceSet) {
		PredicateBitSet staticSet = null;
		int lastI = -1;
		// transform clusterPair  to line pair
//		Iterator<LinePair> iter = clusterPair.getLinePairIterator();
		while (iter.hasNext()) {
			LinePair lPair = iter.next();
//			if (!MMCSDC.calculatedPair.add(lPair)){
//				continue;
//			}

			int i = lPair.getLine1();
			int j = lPair.getLine2();
			int[] row1 = input2s[i];
			if (staticSet == null || i != lastI)
				staticSet = getStatic(pairs, row1);

			int[] row2 = input2s[j];
			if (i != j) {
				PredicateBitSet set = getPredicateSet(staticSet, pairs, row1, row2);
				evidenceSet.add(set);

				PredicateBitSet set2 = getPredicateSet(getStatic(pairs, row2), pairs, row2, row1);
				evidenceSet.add(set2);
			}

			lastI = i;

		}
	}
	public void addEvidences(List<ClusterPair> clusterPair, IEvidenceSet evidenceSet){
		clusterPair.forEach(clusterPair1 -> addEvidences(clusterPair1, evidenceSet));
	}

	public IEvidenceSet buildEvidenceSet(Input input) {
		int[][] input2s = input.getInts();

		Collection<ColumnPair> pairs = predicates.getColumnPairs();
		createSets(pairs);

		IEvidenceSet evidenceSet = new TroveEvidenceSet();
		for (int i = 0; i < input.getLineCount(); ++i) {
			if (i % 10000 == 0)
				System.out.println("Sampling: " + i);
			int[] row1 = input2s[i];
			PredicateBitSet staticSet = getStatic(pairs, row1);
			for (int j = 0; j < input.getLineCount(); ++j) {
				// TODO: need to check this still for same subindex?
				int[] row2 = input2s[j];
				if (i != j) {
					PredicateBitSet set = getPredicateSet(staticSet, pairs, row1, row2);
					evidenceSet.add(set);
				}
			}
		}
		return evidenceSet;

	}

	protected PredicateBitSet getPredicateSet(PredicateBitSet staticSet, Collection<ColumnPair> pairs, int[] row1,
			int[] row2) {
		PredicateBitSet set = PredicateSetFactory.create(staticSet);
		// which predicates are satisfied by these two lines?
		for (ColumnPair p : pairs) {
			PredicateBitSet[] list = map.get(p);
			if (p.isJoinable()) {
				if (equals(row1, row2, p))
					set.addAll(list[0]);
				else
					set.addAll(list[1]);
			}
			if (p.isComparable()) {
				int compare = compare(row1, row2, p);
				if (compare < 0) {
					set.addAll(list[4]);
				} else if (compare == 0) {
					set.addAll(list[5]);
				} else {
					set.addAll(list[6]);
				}

			}

		}
		return set;
	}

	protected PredicateBitSet getStatic(Collection<ColumnPair> pairs, int[] row1) {
		PredicateBitSet set = PredicateSetFactory.create();
		// which predicates are satisfied by these two lines?
		for (ColumnPair p : pairs) {
			if (p.getC1().equals(p.getC2()))
				continue;

			PredicateBitSet[] list = map.get(p);
			if (p.isJoinable()) {
				if (equals(row1, row1, p))
					set.addAll(list[2]);
				else
					set.addAll(list[3]);
			}
			if (p.isComparable()) {
				int compare2 = compare(row1, row1, p);
				if (compare2 < 0) {
					set.addAll(list[7]);
				} else if (compare2 == 0) {
					set.addAll(list[8]);
				} else {
					set.addAll(list[9]);
				}
			}

		}
		return set;
	}

	private int compare(int[] row1, int[] row2, ColumnPair p) {
		return Integer.compare(row1[p.getC1().getIndex()], row2[p.getC2().getIndex()]);
	}

	private boolean equals(int[] row1, int[] row2, ColumnPair p) {
		return row1[p.getC1().getIndex()] == row2[p.getC2().getIndex()];
	}
	

}
