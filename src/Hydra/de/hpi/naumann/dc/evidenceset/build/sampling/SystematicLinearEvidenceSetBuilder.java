package Hydra.de.hpi.naumann.dc.evidenceset.build.sampling;


import HyDCFinalVersion.MMCSDC;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.TroveEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.EvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.ColumnPair;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.LinePair;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import gnu.trove.iterator.TIntIterator;

import java.util.*;

public class SystematicLinearEvidenceSetBuilder extends EvidenceSetBuilder {
	private final int factor;

	private Map<LinePair, PredicateBitSet> linePairPredicateBitSetMap;
	public SystematicLinearEvidenceSetBuilder(PredicateBuilder pred, int factor) {
		super(pred);
		this.factor = factor;
		linePairPredicateBitSetMap = new HashMap<>();
	}

	public IEvidenceSet buildEvidenceSet(Input input) {
		Collection<ColumnPair> pairs = predicates.getColumnPairs();
		createSets(pairs);

		IEvidenceSet evidenceSet = new TroveEvidenceSet();

		Random r = new Random();
		for (int i = 0; i < input.getLineCount(); ++i) {
			PredicateBitSet staticSet = getStatic(pairs, i);
			for (int count = 0; count < factor; ++count) {
				int j = r.nextInt(input.getLineCount() - 1);
				if (j >= i)
					j++;
				LinePair linePair = new LinePair(i, j);
				if (linePairPredicateBitSetMap.containsKey(linePair) ){
					evidenceSet.add(linePairPredicateBitSetMap.get(linePair));
					continue;
				}
				PredicateBitSet set = getPredicateSet(staticSet, pairs, i, j);
				linePairPredicateBitSetMap.put(linePair, set);
				evidenceSet.add(set);

			}
		}
		return evidenceSet;
	}

	public IEvidenceSet buildEvidenceSet(Input input, int rate) {
		Collection<ColumnPair> pairs = predicates.getColumnPairs();
		createSets(pairs);

		IEvidenceSet evidenceSet = new TroveEvidenceSet();

		Random r = new Random();
		for (int i = 0; i < input.getLineCount() / rate; ++i) {
			PredicateBitSet staticSet = getStatic(pairs, i);
			for (int count = 0; count < factor; ++count) {
				int j = r.nextInt(input.getLineCount() - 1);
				if (j >= i)
					j++;

				LinePair linePair = new LinePair(i, j);
				if (linePairPredicateBitSetMap.containsKey(linePair) ){
					evidenceSet.add(linePairPredicateBitSetMap.get(linePair));
					continue;
				}
				PredicateBitSet set = getPredicateSet(staticSet, pairs, i, j);
				linePairPredicateBitSetMap.put(linePair, set);
				evidenceSet.add(set);
			}
		}
		return evidenceSet;
	}

	// get evidenceSet for clusterPair
	public HashEvidenceSet getEvidenceSet(ClusterPair clusterPair){
		Collection<ColumnPair> pairs = predicates.getColumnPairs();
		createSets(pairs);

		HashEvidenceSet evidenceSet = new HashEvidenceSet();
		Cluster c1 = clusterPair.getC1();
		Cluster c2 = clusterPair.getC2();
		TIntIterator iter1 = c1.iterator();
		while(iter1.hasNext()){
			int nextC1 = iter1.next();
			PredicateBitSet staticSet = getStatic(pairs, nextC1);
			TIntIterator iter2 = c2.iterator();
			while(iter2.hasNext()) {
				int nextC2 = iter2.next();
				if ( nextC1 != nextC2){
					PredicateBitSet set = getPredicateSet(staticSet, pairs, nextC1, nextC2);
					evidenceSet.add(set);

					PredicateBitSet set1 = getPredicateSet(staticSet, pairs, nextC2, nextC1);
					evidenceSet.add(set1);
				}

			}
		}

		return evidenceSet;
	}

	public HashEvidenceSet getEvidenceSet(List<ClusterPair> clusterPairs){
		HashEvidenceSet evidenceSet = new HashEvidenceSet();
		clusterPairs.forEach(clusterPair -> evidenceSet.add(getEvidenceSet(clusterPair)));
		return evidenceSet;
	}

}
