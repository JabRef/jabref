package org.jabref.model.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.model.entry.BibEntry;

public class SearchResults {

    private final Map<String, List<SearchResult>> searchResults = new HashMap<>();

    public void mergeSearchResults(SearchResults additionalResults) {
        this.searchResults.putAll(additionalResults.searchResults);
    }

    public void addSearchResult(String entryId, SearchResult result) {
        searchResults.computeIfAbsent(entryId, k -> new ArrayList<>()).add(result);
    }

    public void addSearchResult(Collection<String> entries, SearchResult result) {
        entries.forEach(entry -> addSearchResult(entry, result));
    }

    public float getSearchScoreForEntry(BibEntry entry) {
        if (searchResults.containsKey(entry.getId())) {
            return searchResults.get(entry.getId())
                                .stream()
                                .map(SearchResult::getSearchScore)
                                .reduce(0f, Float::max);
        }
        return 0f;
    }

    public boolean hasFulltextResults(BibEntry entry) {
        if (searchResults.containsKey(entry.getId())) {
            return searchResults.get(entry.getId())
                                .stream()
                                .anyMatch(SearchResult::hasFulltextResults);
        }
        return false;
    }

    public Map<String, List<SearchResult>> getFileSearchResultsForEntry(BibEntry entry) {
        Map<String, List<SearchResult>> results = new HashMap<>();
        if (searchResults.containsKey(entry.getId())) {
            for (SearchResult result : searchResults.get(entry.getId())) {
                if (result.hasFulltextResults()) {
                    results.computeIfAbsent(result.getPath(), k -> new ArrayList<>()).add(result);
                }
            }
        }
        return results;
    }

    public Set<String> getMatchedEntries() {
        return searchResults.keySet();
    }
}
