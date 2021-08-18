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

    public HashMap<String, HashMap<Integer, List<SearchResult>>> getSearchResultsByPathAndPage() {
        HashMap<String, HashMap<Integer, List<SearchResult>>> resultsByPathAndPage = new HashMap<>();
        for (SearchResult result : searchResults) {
            HashMap<Integer, List<SearchResult>> resultsByPage;
            if (resultsByPathAndPage.containsKey(result.getPath())) {
                resultsByPage = resultsByPathAndPage.get(result.getPath());
            } else {
                resultsByPage = new HashMap<>();
                resultsByPathAndPage.put(result.getPath(), resultsByPage);
            }
            if (resultsByPage.containsKey(result.getPageNumber())) {
                resultsByPage.get(result.getPageNumber()).add(result);
            } else {
                resultsByPage.put(result.getPageNumber(), List.of(result));
            }
        }
        return resultsByPathAndPage;
    }

    public int numSearchResults() {
        return this.searchResults.size();
    }
}
