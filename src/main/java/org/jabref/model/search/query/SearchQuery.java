package org.jabref.model.search.query;

import java.util.EnumSet;
import java.util.Objects;

import org.jabref.logic.search.query.SearchQueryConversion;
import org.jabref.model.search.SearchFlags;

import org.antlr.v4.runtime.misc.ParseCancellationException;

public class SearchQuery {

    private final String searchExpression;
    private final EnumSet<SearchFlags> searchFlags;

    private boolean isValidExpression;
    private SearchResults searchResults;

    public SearchQuery(String searchExpression, EnumSet<SearchFlags> searchFlags) {
        this.searchExpression = Objects.requireNonNull(searchExpression);
        this.searchFlags = searchFlags;
        try {
            SearchQueryConversion.getStartContext(searchExpression);
            isValidExpression = true;
        } catch (ParseCancellationException e) {
            isValidExpression = false;
        }
    }

    public String getSearchExpression() {
        return searchExpression;
    }

    public SearchResults getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(SearchResults searchResults) {
        this.searchResults = searchResults;
    }

    public boolean isValid() {
        return isValidExpression;
    }

    public EnumSet<SearchFlags> getSearchFlags() {
        return searchFlags;
    }

    @Override
    public String toString() {
        return searchExpression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SearchQuery that)) {
            return false;
        }
        return Objects.equals(searchExpression, that.searchExpression)
                && Objects.equals(searchFlags, that.searchFlags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(searchExpression, searchFlags);
    }
}
