package Hydra.de.hpi.naumann.dc.helpers;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;

import java.util.*;

public class IndexProvider<T> {
	private Map<T, Integer> indexes = new HashMap<>();
	private List<T> objects = new ArrayList<>();

	private int nextIndex = 0;

	public Integer getIndex(T object) {
		Integer index = indexes.putIfAbsent(object, Integer.valueOf(nextIndex));
		if (index == null) {
			index = Integer.valueOf(nextIndex);
			++nextIndex;
			objects.add(object);
		}
		return index;
	}
	public boolean contains(T object) {
		return indexes.containsKey(object);
	}

	public T getObject(int index) {
		return objects.get(index);
	}

	public IBitSet getBitSet(Iterable<T> objects) {
		IBitSet result = LongBitSet.FACTORY.create();
		for (T i : objects) {
			result.set(getIndex(i).intValue());
		}
		return result;
	}

	public Collection<T> getObjects(IBitSet bitset) {
		ArrayList<T> objects = new ArrayList<>();
		for (int i = bitset.nextSetBit(0); i >= 0; i = bitset.nextSetBit(i + 1)) {
			objects.add(getObject(i));
		}
		return objects;
	}

	public static <A extends Comparable<A>> IndexProvider<A> getSorted(IndexProvider<A> r) {
		IndexProvider<A> sorted = new IndexProvider<>();
		List<A> listC = new ArrayList<A>(r.objects);
		Collections.sort(listC);
		for (A c : listC) {
			sorted.getIndex(c);
		}
		return sorted;
	}

	public int size() {
		return nextIndex;
	}
}
