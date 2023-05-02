package org.jabref.model.pdf.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    public HashMap<String, List<SearchResult>> getSearchResultsByPath() {
        HashMap<String, List<SearchResult>> resultsByPath = new HashMap<>();
        for (SearchResult result : searchResults) {
            if (resultsByPath.containsKey(result.getPath())) {
                resultsByPath.get(result.getPath()).add(result);
            } else {
                List<SearchResult> resultsForPath = new ArrayList<>();
                resultsForPath.add(result);
                resultsByPath.put(result.getPath(), resultsForPath);
            }
        }
        return resultsByPath;
    }

    public int numSearchResults() {
        return this.searchResults.size();
    }
}
