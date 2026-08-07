package org.jabref.logic.bibtex.comparator;

import java.util.Comparator;

import org.jabref.model.entry.BibEntry;

public class BibEntryByCitationKeyComparator implements Comparator<BibEntry> {
    @Override
    public int compare(BibEntry e1, BibEntry e2) {
        boolean e1HasCitationKey = e1.hasCitationKey();
        boolean e2HasCitationKey = e2.hasCitationKey();

        if (!e1HasCitationKey && !e2HasCitationKey) {
            return 0;
        }

        if (e1HasCitationKey && !e2HasCitationKey) {
            return -1;
        }

        if (!e1HasCitationKey && e2HasCitationKey) {
            return 1;
        }

        assert e1HasCitationKey && e2HasCitationKey;

        return e1.getCitationKey().get().compareTo(e2.getCitationKey().get());
    }
}
