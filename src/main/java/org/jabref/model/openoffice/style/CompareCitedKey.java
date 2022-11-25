package org.jabref.model.openoffice.style;

import java.util.Comparator;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

/*
 * Given a Comparator<BibEntry> provide a Comparator<ComparableCitedKey> that also handles
 * unresolved citation keys.
 */
public class CompareCitedKey implements Comparator<ComparableCitedKey> {

    Comparator<BibEntry> entryComparator;
    boolean unresolvedComesFirst;

    CompareCitedKey(Comparator<BibEntry> entryComparator, boolean unresolvedComesFirst) {
        this.entryComparator = entryComparator;
        this.unresolvedComesFirst = unresolvedComesFirst;
    }

    public int compare(ComparableCitedKey a, ComparableCitedKey b) {
        Optional<BibEntry> aBibEntry = a.getBibEntry();
        Optional<BibEntry> bBibEntry = b.getBibEntry();
        final int mul = unresolvedComesFirst ? (+1) : (-1);

        if (aBibEntry.isEmpty() && bBibEntry.isEmpty()) {
            // Both are unresolved: compare them by citation key.
            return a.getCitationKey().compareTo(b.getCitationKey());
        } else if (aBibEntry.isEmpty()) {
            return -mul;
        } else if (bBibEntry.isEmpty()) {
            return mul;
        } else {
            // Proper comparison of entries
            return entryComparator.compare(aBibEntry.get(), bBibEntry.get());
        }
    }
}
