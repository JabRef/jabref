package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;

public class MockRelatedEntriesFetcher implements RelatedEntriesFetcher {
    private int fetchesCount = 0;
    @Override
    public List<BibEntry> fetch(BibEntry entry) {
        fetchesCount++;
        return Collections.emptyList();
    }

    /**
     * Returns the number of times the {@link MockRelatedEntriesFetcher#fetch(BibEntry)} method was called.
     * */
    public int getFetchesCount() {
        return fetchesCount;
    }
}
