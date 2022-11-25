package org.jabref.model.openoffice.util;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OOListUtil {

    public static <T, U> List<U> map(List<T> list, Function<T, U> fun) {
        return list.stream().map(fun).collect(Collectors.toList());
    }

    /**
     * Integers 0..(len-1)
     */
    public static List<Integer> makeIndices(int len) {
        return Stream.iterate(0, i -> i + 1).limit(len).collect(Collectors.toList());
    }

    /**
     * Return indices so that list.get(indices.get(i)) is sorted.
     */
    public static <T extends U, U> List<Integer> order(List<T> list, Comparator<U> comparator) {
        List<Integer> indices = makeIndices(list.size());
        indices.sort((a, b) -> comparator.compare(list.get(a), list.get(b)));
        return indices;
    }
}
