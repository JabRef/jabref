package net.sf.jabref.util;

import java.util.*;

import net.sf.jabref.Util;

/**
 * Class for putting objects of two types together.
 * 
 * The usual way to use this is like this:
 * 
 * <pre>
 * Pair<Integer, String> intStr = new Pair<Integer, String>(5, "Hello");
 * 
 * intStr.p == 5
 * intStr.v == "Hello"
 * </pre>
 * 
 * @author oezbek
 * 
 * @param <P> First type to be contained in the pair.
 * @param <V> Second type to be contained in the pair.
 */
public class Pair<P, V> {

	/**
	 * Direct access to the p element is allowed.
	 */
	public P p;

	/**
	 * Direct access to the v element is allowed.
	 */
	public V v;

	/**
	 * Constructor that sets the given p and v values.
	 * 
	 * @param p
	 * @param v
	 */
	public Pair(P p, V v) {
		this.p = p;
		this.v = v;
	}

	/**
	 * Returns a comparator that compares by p.
	 * 
	 * @param <V>
	 *            The V type is not important for this method.
	 * @param
	 *            <P>
	 *            The P type of the pair needs to be comparable.
	 * @return A comparator for the p in a pair.
	 */
	public static <P extends Comparable<P>, V> Comparator<Pair<P, V>> pCompare() {
		return new Comparator<Pair<P, V>>() {
			public int compare(Pair<P, V> arg0, Pair<P, V> arg1) {
				return arg0.p.compareTo(arg1.p);
			}
		};
	}

	/**
	 * Given a comparator for p elements, returns a Comparator for pairs which
	 * uses this given comparator to make the comparison.
	 * 
	 * @param <V>
	 *            The V-Type of the Pair.
	 * @param
	 *            <P>
	 *            The P-Type of the Pair.
	 * @param comp
	 *            A comparator which will be wrapped so that it can be used to
	 *            compare
	 * @return A comparator for Pairs of type P and V which makes use of the
	 *         given comparator.
	 */
	public static <P> Comparator<? super Pair<P, ?>> pCompare(
		final Comparator<P> comp) {
		return new Comparator<Pair<P, ?>>() {
			public int compare(Pair<P, ?> arg0, Pair<P, ?> arg1) {
				return comp.compare(arg0.p, arg1.p);
			}
		};
	}

	/**
	 * Returns a Pair with the type and the contents of the current Pair flipped
	 * from P to V.
	 * 
	 * <pre>
	 * Pair&lt;Integer, String&gt; intString = new Pair&lt;Integer, String&gt;(5, &quot;Hallo&quot;);
	 * Pair&lt;String, Integer&gt; strInteger = intString.flip();
	 * </pre>
	 */
	public Pair<V, P> flip() {
		return new Pair<V, P>(v, p);
	}

	/**
	 * Returns a list of pairs with P and V being switched.
	 * 
	 * @param
	 * <P>
	 * @param <V>
	 * @param list
	 * @return
	 */
	public static <P, V> List<Pair<V, P>> flipList(List<Pair<P, V>> list) {
		LinkedList<Pair<V, P>> result = new LinkedList<Pair<V, P>>();
		for (Pair<P, V> pair : list)
			result.add(pair.flip());
		return result;
	}

	/**
	 * Given a collection of Pair<P,V> it will put all V values that have the
	 * same P-value together in a set and return a collection of those sets with
	 * their associated P.
	 * 
	 * Example:
	 * 
	 * <pre>
	 * Collection&lt;Pair&lt;Integer, String&gt;&gt; c = new LinkedList&lt;Pair&lt;Integer, String&gt;&gt;();
	 * c.add(new Pair&lt;Integer, String&gt;(3, &quot;Hallo&quot;));
	 * c.add(new Pair&lt;Integer, String&gt;(4, &quot;Bye&quot;));
	 * c.add(new Pair&lt;Integer, String&gt;(3, &quot;Adios&quot;));
	 * 
	 * Collection&lt;Pair&lt;Integer, Set&lt;String&gt;&gt;&gt; result = Pair.disjointPartition(c);
	 * 
	 * result == [(3, [&quot;Hallo&quot;, &quot;Adios&quot;]), (4, [&quot;Bye&quot;])]
	 * </pre>
	 * 
	 * @param
	 * <P>
	 * @param <V>
	 * @param list
	 * @return
	 */
	public static <P extends Comparable<P>, V> List<Pair<P, Set<V>>> disjointPartition(
		List<Pair<P, V>> list) {

		List<Pair<P, Set<V>>> result = new LinkedList<Pair<P, Set<V>>>();

		Comparator<Pair<P, V>> c = Pair.pCompare();
		Collections.sort(list, Collections.reverseOrder(c));

		Iterator<Pair<P, V>> i = list.iterator();

		Set<V> vs;

		if (i.hasNext()) {
			Pair<P, V> first = i.next();
			P last = first.p;
			vs = new HashSet<V>();
			vs.add(first.v);

			while (i.hasNext()) {
				Pair<P, V> next = i.next();
				if (last.compareTo(next.p) == 0) {
					vs.add(next.v);
				} else {
					result.add(new Pair<P, Set<V>>(last, vs));
					vs = new HashSet<V>();
					last = next.p;
					vs.add(next.v);
				}
			}
			result.add(new Pair<P, Set<V>>(last, vs));
		}
		return result;

	}

	/**
	 * Returns a comparator that compares by v.
	 * 
	 * @param
	 * <P>
	 * The P type is not important for this method.
	 * @param <V>
	 *            The V type of the pair needs to be comparable.
	 * @return A comparator for the v in a pair.
	 */
	public static <V extends Comparable<V>> Comparator<? super Pair<?, V>> vCompare() {
		return new Comparator<Pair<?, V>>() {
			public int compare(Pair<?, V> arg0, Pair<?, V> arg1) {
				return arg0.v.compareTo(arg1.v);
			}
		};
	}

	/**
	 * Given a comparator for v elements, returns a Comparator for pairs which
	 * uses this given comparator to make the comparison.
	 * 
	 * @param
	 * <P>
	 * The P-Type of the Pair.
	 * @param <V>
	 *            The V-Type of the Pair.
	 * @param vComp
	 *            A comparator which will be wrapped so that it can be used to
	 *            compare
	 * @return A comparator for Pairs of type P and V which makes use of the
	 *         given comparator.
	 */
	public static <V> Comparator<? super Pair<?,V>> vCompare(
		final Comparator<V> vComp) {
		return new Comparator<Pair<?, V>>() {
			public int compare(Pair<?, V> arg0, Pair<?, V> arg1) {
				return vComp.compare(arg0.v, arg1.v);
			}
		};
	}

	/**
	 * Takes a list of P and list of V and returns a list of Pair<P,V>. If the
	 * lengths of the lists differ missing values are padded by null.
	 * 
	 * @param
	 * <P>
	 * @param <V>
	 * @param ps
	 * @param vs
	 * @return
	 */
	public static <P, V> List<Pair<P, V>> zip(List<P> ps, List<V> vs) {

		List<Pair<P, V>> result = new LinkedList<Pair<P, V>>();

		Iterator<P> pI = ps.iterator();
		Iterator<V> vI = vs.iterator();

		while (pI.hasNext()) {
			V nextV = (vI.hasNext() ? vI.next() : null);
			result.add(new Pair<P, V>(pI.next(), nextV));
		}
		while (vI.hasNext()) {
			result.add(new Pair<P, V>(null, vI.next()));
		}
		return result;
	}

	/**
	 * Unzips the given pair list by returning a list of the p values.
	 * 
	 * @param
	 * <P>
	 * @param <V>
	 * @param list
	 * @return
	 */
	public static <P> List<P> pList(List<? extends Pair<P, ?>> list) {

		List<P> result = new LinkedList<P>();

		for (Pair<P, ?> pair : list) {
			result.add(pair.p);
		}
		return result;
	}

	/**
	 * Unzips the given pair by return a list of the v values.
	 * 
	 * @param <V>
	 * @param list
	 * @return
	 */
	public static <V> List<V> vList(List<? extends Pair<?, V>> list) {

		List<V> result = new LinkedList<V>();

		for (Pair<?, V> pair : list) {
			result.add(pair.v);
		}
		return result;
	}

	
	/**
	 * Given an Iterator in Pair<P,V>, will return an Iterator in V that proxies all calls to the given iterator.
	 */
	public static <V> Iterator<V> iteratorV(final Iterator<? extends Pair<?, V>> iterator){
		return new Iterator<V>(){
			public boolean hasNext() {
				return iterator.hasNext();
			}

			public V next() {
				return iterator.next().v;
			}

			public void remove() {
				iterator.remove();
			}
		};
	}
	
	/**
	 * Given an iterable in Pair<P,V> will return an iterable in P.
	 */
	public static <V> Iterable<V> iterableV(final Iterable<? extends Pair<?, V>> iterable){
		return new Iterable<V>(){
			public Iterator<V> iterator() {
				return iteratorV(iterable.iterator());
			}
		};
	}
	
	/**
	 * Given an Iterator in Pair<P,V>, will return an Iterator in P that proxies all calls to the given iterator.
	 */
	public static <P> Iterator<P> iteratorP(final Iterator<? extends Pair<P,?>> iterator){
		return new Iterator<P>(){
			public boolean hasNext() {
				return iterator.hasNext();
			}

			public P next() {
				return iterator.next().p;
			}

			public void remove() {
				iterator.remove();
			}
		};
	}
	
	/**
	 * Given an iterable in Pair<P,V> will return an iterable in P.
	 */
	public static <P> Iterable<P> iterableP(final Iterable<? extends Pair<P,?>> iterable){
		return new Iterable<P>(){
			public Iterator<P> iterator() {
				return iteratorP(iterable.iterator());
			}
		};
	}
	
	/**
	 * Debugging Output method using the toString method of P and V.
	 */
	public String toString() {
		return new StringBuffer().append('<').append(p).append(',').append(v)
			.append('>').toString();
	}

	@Override
	public int hashCode() {
		return (this.p == null ? 0 : this.p.hashCode())
			| (this.v == null ? 0 : this.v.hashCode());
	}

	/**
	 * Returns true if both the p and v of the given pair equal p and v in the
	 * current Pair.
	 */
	public boolean equals(Object o) {
		if (!(o instanceof Pair))
			return false;

		Pair<?, ?> other = (Pair<?, ?>) o;
		return Util.equals(this.p, other.p) && Util.equals(this.v, other.v);
	}

}
