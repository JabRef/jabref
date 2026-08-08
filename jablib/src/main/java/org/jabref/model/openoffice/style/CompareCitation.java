package org.jabref.model.openoffice.style;

import java.util.Comparator;

import org.jabref.model.entry.BibEntry;

/*
 * Given a Comparator<BibEntry> provide a Comparator<ComparableCitation> that can handle unresolved
 * citation keys and takes pageInfo into account.
 */
public class CompareCitation implements Comparator<ComparableCitation> {

    private final CompareCitedReference citedReferenceComparator;

    CompareCitation(Comparator<BibEntry> entryComparator, boolean unresolvedComesFirst) {
        this.citedReferenceComparator = new CompareCitedReference(entryComparator, unresolvedComesFirst);
    }

    public int compare(ComparableCitation a, ComparableCitation b) {
        int res = citedReferenceComparator.compare(a, b);

        // Also consider pageInfo
        if (res == 0) {
            res = PageInfo.comparePageInfo(a.getPageInfo(), b.getPageInfo());
        }
        return res;
    }
}


