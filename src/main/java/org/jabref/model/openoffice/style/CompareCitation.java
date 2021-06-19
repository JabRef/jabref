package org.jabref.model.openoffice.style;

import java.util.Comparator;

import org.jabref.model.entry.BibEntry;

/*
 * Given a Comparator<BibEntry> provide a Comparator<ComparableCitation> that can handle unresolved
 * citation keys and takes pageInfo into account.
 */
public class CompareCitation implements Comparator<ComparableCitation> {

    private CompareCitedKey citedKeyComparator;

    CompareCitation(Comparator<BibEntry> entryComparator, boolean unresolvedComesFirst) {
        this.citedKeyComparator = new CompareCitedKey(entryComparator, unresolvedComesFirst);
    }

    public int compare(ComparableCitation a, ComparableCitation b) {
        int res = citedKeyComparator.compare(a, b);

        // Also consider pageInfo
        if (res == 0) {
            res = PageInfo.comparePageInfo(a.getPageInfo(), b.getPageInfo());
        }
        return res;
    }
}


