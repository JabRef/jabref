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
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchQuery.class);

    private final String searchExpression;
    private final EnumSet<SearchFlags> searchFlags;
    private SearchParser.StartContext context;
    private boolean isValidExpression;
    private SearchResults searchResults;

    public SearchQuery(String searchExpression) {
        this(searchExpression, EnumSet.noneOf(SearchFlags.class));
    }

    public SearchQuery(@NonNull String searchExpression, EnumSet<SearchFlags> searchFlags) {
        this.searchExpression = searchExpression;
        this.searchFlags = searchFlags;
        try {
            this.context = getStartContext(searchExpression);
            isValidExpression = true;
        } catch (ParseCancellationException e) {
            // We use getCause here as the real exception is nested and this avoids that the stack trace get too large
            // and we don't see the root cause
            LOGGER.error("Search query Parsing error", e.getCause());
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
