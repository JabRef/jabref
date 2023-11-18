package org.jabref.model.util;

import java.util.function.Predicate;

/**
 * Provides a few helper methods for lists.
 */
public class ListUtil {

    /**
     * Equivalent to list.stream().anyMatch but with slightly better performance (especially for small lists).
     */
    public static <T> boolean anyMatch(Iterable<T> list, Predicate<T> predicate) {
        for (T element : list) {
            if (predicate.test(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Equivalent to list.stream().allMatch but with slightly better performance (especially for small lists).
     */
    public static <T> boolean allMatch(Iterable<T> list, Predicate<T> predicate) {
        for (T element : list) {
            if (!predicate.test(element)) {
                return false;
            }
        }
        return true;
    }
}
