package org.jabref.model.search.query;

import java.util.Objects;

public class SqlSearchQuery {

    private final String query;
    private SearchResults searchResults;

    public SqlSearchQuery(String query) {
        this.query = Objects.requireNonNull(query);
    }

    public String getQuery() {
        return query;
    }

    public SearchResults getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(SearchResults searchResults) {
        this.searchResults = searchResults;
    }
}
