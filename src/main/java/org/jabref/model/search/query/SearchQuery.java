package org.jabref.model.search.query;

import java.util.EnumSet;
import java.util.Objects;

import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.ThrowingErrorListener;
import org.jabref.search.SearchLexer;
import org.jabref.search.SearchParser;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;

public class SearchQuery {

    private final String searchExpression;
    private final EnumSet<SearchFlags> searchFlags;
    private SearchParser.StartContext context;
    private boolean isValidExpression;
    private SearchResults searchResults;

    public SearchQuery(String searchExpression) {
        this(searchExpression, EnumSet.noneOf(SearchFlags.class));
    }

    public SearchQuery(String searchExpression, EnumSet<SearchFlags> searchFlags) {
        this.searchExpression = Objects.requireNonNull(searchExpression);
        this.searchFlags = searchFlags;
        try {
            this.context = getStartContext(searchExpression);
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

    public SearchParser.StartContext getContext() {
        return context;
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

    public static SearchParser.StartContext getStartContext(String searchExpression) {
        SearchLexer lexer = new SearchLexer(CharStreams.fromString(searchExpression));
        lexer.removeErrorListeners(); // no infos on file system
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
        SearchParser parser = new SearchParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners(); // no infos on file system
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);
        parser.setErrorHandler(new BailErrorStrategy()); // ParseCancellationException on parse errors
        return parser.start();
    }
}
