package org.jabref.model.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.model.entry.BibEntry;

public class SearchResults {

    private final Map<BibEntry, List<SearchResult>> searchResults = new HashMap<>();

    public void mergeSearchResults(SearchResults additionalResults) {
        this.searchResults.putAll(additionalResults.searchResults);
    }

    public void addSearchResult(BibEntry entry, SearchResult result) {
        searchResults.computeIfAbsent(entry, k -> new ArrayList<>()).add(result);
    }

    public void addSearchResult(Collection<BibEntry> entries, SearchResult result) {
        entries.forEach(entry -> addSearchResult(entry, result));
    }

    public float getSearchScoreForEntry(BibEntry entry) {
        return searchResults.containsKey(entry) ?
                searchResults.get(entry).stream()
                             .map(SearchResult::getLuceneScore)
                             .reduce(0f, Float::sum) : 0f;
    }

    public boolean hasFulltextResults(BibEntry entry) {
        if (searchResults.containsKey(entry)) {
            return searchResults.get(entry).stream().anyMatch(SearchResult::hasFulltextResults);
        }
        return false;
    }

    public Map<String, List<SearchResult>> getFileSearchResultsForEntry(BibEntry entry) {
        Map<String, List<SearchResult>> results = new HashMap<>();
        if (searchResults.containsKey(entry)) {
            for (SearchResult result : searchResults.get(entry)) {
                if (result.hasFulltextResults()) {
                    results.computeIfAbsent(result.getPath(), k -> new ArrayList<>()).add(result);
                }
            }
        }
        return results;
    }

    public Set<BibEntry> getMatchedEntries() {
        return searchResults.keySet();
    }

    public int getNumberOfResults() {
        return searchResults.size();
    }
}
