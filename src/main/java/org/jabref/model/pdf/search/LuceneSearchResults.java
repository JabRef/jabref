package org.jabref.model.pdf.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public final class LuceneSearchResults {

    private final List<SearchResult> searchResults = new LinkedList<>();

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

    public void addResult(SearchResult result) {
        this.searchResults.add(result);
    }

    public float getSearchScore() {
        return this.searchResults.stream().map(SearchResult::getLuceneScore).max(Comparator.comparing(Float::floatValue)).orElse(Float.valueOf(0));
    }
}
