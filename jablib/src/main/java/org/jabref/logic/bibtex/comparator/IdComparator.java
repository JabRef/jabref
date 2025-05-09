package org.jabref.logic.bibtex.comparator;

import java.util.Comparator;

import org.jabref.model.entry.BibEntry;

/**
 * Comparator for sorting BibEntry objects based on their ID. This
 * can be used to sort entries back into the order they were created,
 * provided the IDs given to entries are lexically monotonically increasing.
 */
public class IdComparator implements Comparator<BibEntry> {

    @Override
    public int compare(BibEntry one, BibEntry two) {
        return one.getId().compareTo(two.getId());
    }
}
