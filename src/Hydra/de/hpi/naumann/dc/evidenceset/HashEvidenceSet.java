package Hydra.de.hpi.naumann.dc.evidenceset;

import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class HashEvidenceSet implements IEvidenceSet {

	private Set<PredicateBitSet> evidences = new HashSet<>();

	@Override
	public boolean add(PredicateBitSet predicateSet) {
		return evidences.add(predicateSet);
	}

	@Override
	public boolean add(PredicateBitSet predicateSet, long count) {
		return evidences.add(predicateSet);
	}

	@Override
	public long getCount(PredicateBitSet predicateSet) {
		return evidences.contains(predicateSet) ? 1 : 0;
	}


	@Override
	public Iterator<PredicateBitSet> iterator() {
		return evidences.iterator();
	}

	@Override
	public Set<PredicateBitSet> getSetOfPredicateSets() {
		return evidences;
	}

	@Override
	public int size() {
		return evidences.size();
	}

	@Override
	public boolean isEmpty() {
		return evidences.isEmpty();
	}

	// TODO: for evidenceSet add if it will cost too much time
	public void add(HashEvidenceSet hashEvidenceSet){
		evidences.addAll(hashEvidenceSet.getSetOfPredicateSets());
	}

}
