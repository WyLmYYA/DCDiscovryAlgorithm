package Hydra.de.hpi.naumann.dc.evidenceset.build;

import HyDCFinalVersion.MMCSDC;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.TroveEvidenceSet;
import Hydra.de.hpi.naumann.dc.input.ColumnPair;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.LinePair;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateSetFactory;
import utils.TimeCal2;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class PartitionEvidenceSetBuilder extends EvidenceSetBuilder {
	private Collection<ColumnPair> pairs;
	private int[][] input2s;

	public PartitionEvidenceSetBuilder(PredicateBuilder predicates2, int[][] values) {
		super(predicates2);
		pairs = predicates.getColumnPairs();
		createSets(pairs);
		this.input2s = values;
	}

	public void addEvidences(ClusterPair clusterPair, IEvidenceSet evidenceSet) {

		PredicateBitSet staticSet = null;
		int lastI = -1;
		// transform clusterPair  to line pair

		Iterator<LinePair> iter = clusterPair.getLinePairIterator();


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

				TimeCal2.add(1, 3);
				PredicateBitSet set = getPredicateSet(staticSet, pairs, row1, row2);
				evidenceSet.add(set);

				PredicateBitSet set2 = getPredicateSet(getStatic(pairs, row2), pairs, row2, row1);
				evidenceSet.add(set2);

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
