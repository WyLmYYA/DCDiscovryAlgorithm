package Hydra.de.hpi.naumann.dc.cover;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.ch.javasoft.bitset.search.ITreeSearch;
import Hydra.ch.javasoft.bitset.search.TranslatingTreeSearch;
import Hydra.ch.javasoft.bitset.search.TreeSearch;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateSetFactory;

import java.util.*;

public class PrefixMinimalCoverSearch {

	// private BitSetTranslator translator;
	private List<IBitSet> bitsetList = new ArrayList<>();

	private final Collection<IBitSet> startBitsets;

	private TranslatingTreeSearch posCover;

	public PrefixMinimalCoverSearch(PredicateBuilder predicates2) {
		this(predicates2, null);
		this.startBitsets.add(LongBitSet.FACTORY.create());
	}

	//改成超图覆盖
	private PrefixMinimalCoverSearch(PredicateBuilder predicates2, TranslatingTreeSearch tree) {


		for (Collection<Predicate> pSet : predicates2.getPredicateGroups()) {
			IBitSet bitset = LongBitSet.FACTORY.create();
			for (Predicate p : pSet) {
//				System.out.println(p);
				bitset.or(PredicateSetFactory.create(p).getBitset());
			}
			bitsetList.add(bitset);
		}

		this.startBitsets = new ArrayList<IBitSet>();
		this.posCover = tree;
	}

	private Collection<IBitSet> getBitsets(IEvidenceSet evidenceSet) {
		System.out.println("Evidence Set size: " + evidenceSet.size());
		if (posCover == null) {
			int[] counts = getCounts(evidenceSet);
			posCover = new TranslatingTreeSearch(counts, bitsetList);
		}

		System.out.println("Building new bitsets..");
		List<IBitSet> sortedNegCover = new ArrayList<IBitSet>();
		for (PredicateBitSet ps : evidenceSet) {
			sortedNegCover.add(ps.getBitset());
		}

		System.out.println("Sorting new bitsets..");
		sortedNegCover = minimize(sortedNegCover);

		mostGeneralDCs(posCover);

		Collections.sort(sortedNegCover, posCover.getComparator());
		System.out.println("Finished sorting neg 2. list size:" + sortedNegCover.size());

		for (int i = 0; i < sortedNegCover.size(); ++i) {
			posCover.handleInvalid(sortedNegCover.get(i));
//			if (i % 1000 == 0 && i > 0)
//				System.out.println("\r" + i);
		}
		Collection<IBitSet> result = new ArrayList<IBitSet>();
		posCover.forEach(bs -> result.add(bs));

		return result;
	}

	public DenialConstraintSet getDenialConstraints(IEvidenceSet evidenceSet) {
//		System.out.println("    "+evidenceSet.size());//6

		DenialConstraintSet set = new DenialConstraintSet();
		getBitsets(evidenceSet).forEach(valid -> {
			set.add(new DenialConstraint(PredicateSetFactory.create(valid)));
			//System.out.println(valid.toBitSet());

		});
//		for(DenialConstraint dc:set)
//			System.out.println(dc);
//		System.out.println(set.size());
		return set;
	}

	private int[] getCounts(IEvidenceSet evidenceSet) {

		int[] counts = new int[PredicateBitSet.indexProvider.size()];
		for (PredicateBitSet ps : evidenceSet) {
			IBitSet bitset = ps.getBitset();
			for (int i = bitset.nextSetBit(0); i >= 0; i = bitset.nextSetBit(i + 1)) {
				counts[i]++;
			}
		}
		return counts;
	}

	//第一次的minimize
	private List<IBitSet> minimize(final List<IBitSet> sortedNegCover) {
		Collections.sort(sortedNegCover, new Comparator<IBitSet>() {
			@Override
			public int compare(IBitSet o1, IBitSet o2) {
				int erg = Integer.compare(o2.cardinality(), o1.cardinality());
				return erg != 0 ? erg : o2.compareTo(o1);
			}
		});

		System.out.println("starting inverting size " + sortedNegCover.size());
		TreeSearch neg = new TreeSearch();
		sortedNegCover.stream().forEach(invalid -> addInvalidToNeg(neg, invalid));

		final ArrayList<IBitSet> list = new ArrayList<IBitSet>();
		neg.forEach(invalidFD -> list.add(invalidFD));
//		System.out.println("first minimize");
//		for(IBitSet bit: sortedNegCover)
//			System.out.println(bit.toBitSet());
		return list;
	}

	private void mostGeneralDCs(ITreeSearch posCover) {
		for (IBitSet start : startBitsets) {
			posCover.add(start);
		}
	}

	private void addInvalidToNeg(TreeSearch neg, IBitSet invalid) {
		if (neg.findSuperSet(invalid) != null)
			return;

		neg.getAndRemoveGeneralizations(invalid);
		neg.add(invalid);
	}

}
