package org.jabref.model.pdf.search;

import java.util.Collections;
import java.util.List;

public final class ResultSet {

    private final List<SearchResult> searchResults;


    public ResultSet(List<SearchResult> search) {
        this.searchResults = Collections.unmodifiableList(search);
    }

    public ResultSet() {
        this.searchResults = Collections.unmodifiableList(Collections.emptyList());
    }

    private void sortByHits() {
        //TODO implement sorting
    }

    private void sortByAlphabet() {
        //TODO implement sorting
    }

    public List<SearchResult> getSearchResults() {
        return this.searchResults;
    }

    public int numSearchResults() {
        return this.searchResults.size();
    }
}
