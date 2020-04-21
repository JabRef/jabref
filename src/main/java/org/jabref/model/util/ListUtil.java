package org.jabref.model.util;

import java.util.function.Predicate;

public class ListUtil {
    public static <T> boolean anyMatch(Iterable<T> list, Predicate<T> predicate) {
        for (T element : list) {
            if (predicate.test(element)) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean allMatch(Iterable<T> list, Predicate<T> predicate) {
        for (T element : list) {
            if (!predicate.test(element)) {
                return false;
            }
        }
        return true;
    }
}
