package org.jabref.model.openoffice.style;

import java.util.Comparator;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/*
 * Given a Comparator<BibEntry> provide a Comparator<ComparableCitedReference> that also handles
 * unresolved citation keys.
 */
@NullMarked
public class CompareCitedReference implements Comparator<ComparableCitedReference> {

    private final Comparator<BibEntry> entryComparator;
    private final boolean unresolvedComesFirst;

    CompareCitedReference(Comparator<BibEntry> entryComparator, boolean unresolvedComesFirst) {
        this.entryComparator = entryComparator;
        this.unresolvedComesFirst = unresolvedComesFirst;
    }

    public int compare(ComparableCitedReference firstReference, ComparableCitedReference secondReference) {
        Optional<BibEntry> firstBibEntry = firstReference.getBibEntry();
        Optional<BibEntry> secondBibEntry = secondReference.getBibEntry();
        final int unresolvedSortDirection = unresolvedComesFirst ? (+1) : -1;

        if (firstBibEntry.isEmpty() && secondBibEntry.isEmpty()) {
            // Both are unresolved: compare them by citation key.
            return firstReference.getCitationKey().compareTo(secondReference.getCitationKey());
        } else if (firstBibEntry.isEmpty()) {
            return -unresolvedSortDirection;
        } else if (secondBibEntry.isEmpty()) {
            return unresolvedSortDirection;
        } else {
            // Proper comparison of entries
            return entryComparator.compare(firstBibEntry.get(), secondBibEntry.get());
        }
    }
}
