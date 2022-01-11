package Hydra.de.hpi.naumann.dc.predicates.sets;

import HyDCFinalVersion.RunHyDCFinalVersion;
import Hydra.ch.javasoft.bitset.BitSetFactory;
import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet.LongBitSetFactory;
import Hydra.de.hpi.naumann.dc.helpers.IndexProvider;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;

import java.util.Iterator;

public class PredicateBitSet implements Iterable<Predicate> {

	private IBitSet bitset;

	public int allCover = 0;

	private int value = -1;

	public int getValue(){
		if (value != -1)return value;
		else {
			for (int ne = bitset.nextSetBit(0); ne != -1; ne = bitset.nextSetBit(ne + 1)){
				value += RunHyDCFinalVersion.predicateIntegerMap.get(indexProvider.getObject(ne));
			}
		}
		return value;
	}

	public PredicateBitSet() {
		this.bitset = bf.create();
	}

	public PredicateBitSet(IBitSet bitset) {
		this.bitset = bitset.clone();
	}

	public PredicateBitSet(PredicateBitSet pS) {
		this.bitset = pS.getBitset().clone();
	}

	public PredicateBitSet(Predicate p) {
		this.bitset = getBitSet(p);
	}
	
	public void remove(Predicate predicate) {
		this.bitset.clear(indexProvider.getIndex(predicate).intValue());
	}

	public boolean containsPredicate(Predicate predicate) {
		return this.bitset.get(indexProvider.getIndex(predicate).intValue());
	}

	public boolean isSubsetOf(PredicateBitSet superset) {
		return this.bitset.isSubSetOf(superset.getBitset());
	}


	public IBitSet getBitset() {
		return bitset;
	}


	public static IndexProvider<Predicate> indexProvider = new IndexProvider<>();

	private static BitSetFactory bf = new LongBitSetFactory();

	static public Predicate getPredicate(int index) {
		return indexProvider.getObject(index);
	}

	static public IBitSet getBitSet(Predicate p) {
		int index = indexProvider.getIndex(p).intValue();
		IBitSet bitset = bf.create();
		bitset.set(index);
		return bitset;
	}

	public void addAll(PredicateBitSet PredicateBitSet) {
		this.bitset.or(PredicateBitSet.getBitset());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bitset == null) ? 0 : bitset.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PredicateBitSet other = (PredicateBitSet) obj;
		if (bitset == null) {
			if (other.bitset != null)
				return false;
		} else if (!bitset.equals(other.bitset))
			return false;
		return true;
	}

	public static int getIndex(Predicate add) {
		return indexProvider.getIndex(add).intValue();
	}

	public int size() {
		return this.bitset.cardinality();
	}

	@Override
	public Iterator<Predicate> iterator() {
		return new Iterator<Predicate>() {
			private int currentIndex = bitset.nextSetBit(0);
			
			@Override
			public Predicate next() {
				int lastIndex = currentIndex;
				currentIndex = bitset.nextSetBit(currentIndex + 1);
				return indexProvider.getObject(lastIndex);
			}
			
			@Override
			public boolean hasNext() {
				return currentIndex >= 0;
			}
		};
	}
	public static final String NOT = "\u00AC";//¬
	public static final String AND = "∧";
	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append(NOT+"("/*+getPredicate(0).toString()*/);
		int count=1;
		for (Predicate predicate : this) {
			if(count!=1)
				s.append(AND+predicate.toString());
			else
				s.append(predicate.toString());
			count++;
		}
		return s.toString();
	}


	public boolean add(Predicate predicate) {
		int index = getIndex(predicate);
		boolean newAdded = !bitset.get(index);
		this.bitset.set(index);
		return newAdded;
	}

}
