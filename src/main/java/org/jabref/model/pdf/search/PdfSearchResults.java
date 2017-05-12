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
        this.searchResults = Collections.unmodifiableList(Collections.emptyList());
    }

    public List<SearchResult> getSortedByScore() {
        List<SearchResult> sortedList = new ArrayList<>(searchResults);
        sortedList.sort((searchResult, t1) -> {
            if (searchResult.getLuceneScore() < t1.getLuceneScore()) {
                return -1;
            }
            if (searchResult.getLuceneScore() > t1.getLuceneScore()) {
                return 1;
            }
            return 0;
        });
        return Collections.unmodifiableList(sortedList);
    }

    private List<SearchResult> getSortedByAlphabet() {
        //TODO implement sorting
        throw new RuntimeException("Not implemented");
    }

    public List<SearchResult> getSearchResults() {
        return this.searchResults;
    }

    public int numSearchResults() {
        return this.searchResults.size();
    }
}
