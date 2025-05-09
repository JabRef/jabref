package org.jabref.model.util;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

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

    /**
     * Extract all {@link LinkedFile}s from a list of {@link BibEntry}s.
     * The result is a stream of distinct {@link LinkedFile}s.
     */
    public static Stream<LinkedFile> getLinkedFiles(Iterable<BibEntry> entries) {
        return StreamSupport
                .stream(entries.spliterator(), false)
                .map(BibEntry::getFiles)
                .flatMap(List::stream)
                .distinct();
    }
}
