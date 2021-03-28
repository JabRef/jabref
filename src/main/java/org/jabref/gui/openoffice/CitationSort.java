package org.jabref.gui.openoffice;

import java.util.Comparator;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

class CitationSort {

    interface ComparableCitation {
        public String getCitationKey();
        public Optional<BibEntry> getBibEntry();
    }

    static class CitationComparator implements Comparator<ComparableCitation> {

        Comparator<BibEntry> entryComparator;
        boolean unresolvedComesFirst;

        CitationComparator(Comparator<BibEntry> entryComparator,
                           boolean unresolvedComesFirst) {
            this.entryComparator = entryComparator;
            this.unresolvedComesFirst = unresolvedComesFirst;
        }

        public int compare(ComparableCitation a, ComparableCitation b) {
            Optional<BibEntry> abe = a.getBibEntry();
            Optional<BibEntry> bbe = b.getBibEntry();

            if (abe.isEmpty() && bbe.isEmpty()) {
                // Both are unresolved: compare them by citation key.
                String ack = a.getCitationKey();
                String bck = b.getCitationKey();
                return ack.compareTo(bck);
            }
            // Comparing unresolved and real entry

            final int mul = unresolvedComesFirst ? (+1) : (-1);
            if (abe.isEmpty()) {
                return -mul;
            }
            if (bbe.isEmpty()) {
                return mul;
            }
            // Proper comparison of entries
            return entryComparator.compare(abe.get(),
                                           bbe.get());
        }
    }

}
