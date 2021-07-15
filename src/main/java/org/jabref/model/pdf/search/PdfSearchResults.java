package org.jabref.model.pdf.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PdfSearchResults {

    private final List<SearchResult> searchResults;

    public PdfSearchResults(List<SearchResult> search) {
        this.searchResults = Collections.unmodifiableList(search);
    }

    public PdfSearchResults() {
        this.searchResults = Collections.emptyList();
    }

    public List<SearchResult> getSortedByScore() {
        List<SearchResult> sortedList = new ArrayList<>(searchResults);
        sortedList.sort((searchResult, t1) -> Float.compare(searchResult.getLuceneScore(), t1.getLuceneScore()));
        return Collections.unmodifiableList(sortedList);
    }

    public List<SearchResult> getSearchResults() {
        return this.searchResults;
    }

    public int numSearchResults() {
        return this.searchResults.size();
    }
}
