package Hydra.de.hpi.naumann.dc.evidenceset.build.sampling;

import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.TroveEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.EvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.ColumnPair;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;

import java.util.Collection;
import java.util.Random;

public class SystematicLinearEvidenceSetBuilder extends EvidenceSetBuilder {
	private final int factor;

	public SystematicLinearEvidenceSetBuilder(PredicateBuilder pred, int factor) {
		super(pred);
		this.factor = factor;
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
				PredicateBitSet set = getPredicateSet(staticSet, pairs, i, j);
				evidenceSet.add(set);
			}
		}
		return evidenceSet;
	}

}
