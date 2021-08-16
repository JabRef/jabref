package org.jabref.model.openoffice.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OOListUtil {

    public static <T, U> List<U> map(List<T> list, Function<T, U> fun) {
        return list.stream().map(e -> fun.apply(e)).collect(Collectors.toList());
    }

    /** Integers 0..(n-1) */
    public static List<Integer> makeIndices(int n) {
        return Stream.iterate(0, i -> i + 1).limit(n).collect(Collectors.toList());
    }

    /** Return indices so that list.get(indices.get(i)) is sorted. */
    public static <T extends U, U> List<Integer> order(List<T> list, Comparator<U> comparator) {
        List<Integer> ii = makeIndices(list.size());
        Collections.sort(ii, new Comparator<Integer>() {
                @Override public int compare(final Integer o1, final Integer o2) {
                    return comparator.compare((U) list.get(o1), (U) list.get(o2));
                }
            });
        return ii;
    }
}
