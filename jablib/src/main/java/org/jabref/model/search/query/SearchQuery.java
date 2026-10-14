package org.jabref.model.search.query;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.ThrowingErrorListener;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchLexer;
import org.jabref.search.SearchParser;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchQuery.class);

    private final String searchExpression;
    private String normalizedSearchExpression = "";
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
        String resolved = normalizeDoi(searchExpression, searchFlags);
        try {
            this.context = getStartContext(resolved);
            this.normalizedSearchExpression = resolved;
            isValidExpression = containsOnlyValidRegularExpressions(context, searchFlags);
        } catch (ParseCancellationException e) {
            // We use getCause here as the real exception is nested and this avoids that the stack trace get too large
            // and we don't see the root cause
            LOGGER.debug("Search query Parsing error", e.getCause());
            this.normalizedSearchExpression = searchExpression;
            isValidExpression = false;
        }
    }

    /// Returns the original, user-facing search expression.
    public String getSearchExpression() {
        return searchExpression;
    }

    /// Returns the normalized search expression used for parsing and matching
    /// (e.g., with full DOI links normalized to plain DOI identifiers).
    public String getNormalizedSearchExpression() {
        return normalizedSearchExpression;
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

    private static boolean containsOnlyValidRegularExpressions(SearchParser.StartContext context, EnumSet<SearchFlags> searchFlags) {
        return new SearchRegularExpressionValidator(searchFlags).visit(context);
    }

    // [impl->req~search.doi-link-normalization~1]
    /// Normalizes full DOI URLs to plain DOI identifiers (e.g., "https://doi.org/10.1000/182" -> "10.1000/182").
    /// Normalizing to the plain DOI allows matching both entries storing clean DOIs and entries retaining
    /// full HTTP links (since the plain DOI is a substring of the full URL).
    /// Regex mode is skipped because `DOI.parse` strips backslashes and special characters, mutating regex syntax.
    private static String normalizeDoi(String searchExpression, EnumSet<SearchFlags> searchFlags) {
        if (searchFlags.contains(SearchFlags.REGULAR_EXPRESSION) || searchExpression.trim().contains(" ")) {
            return searchExpression;
        }
        String lowerCased = searchExpression.trim().toLowerCase(Locale.ROOT);
        if (lowerCased.startsWith("http://") || lowerCased.startsWith("https://")
                || lowerCased.startsWith("doi.org/") || lowerCased.startsWith("doi:")) {
            return DOI.parse(searchExpression.trim())
                      .map(DOI::asString)
                      .orElse(searchExpression);
        }
        return searchExpression;
    }

    /// Unescapes search value based on the Search grammar rules.
    ///
    /// - STRING_LITERAL: Removes enclosing quotes and unescapes `\"`
    ///
    /// - TERM: Unescapes `\=, \!, \~, \(, \)`
    public static String unescapeSearchValue(SearchParser.SearchValueContext ctx) {
        if (ctx == null) {
            return "";
        }

        String term = ctx.getText();

        if (ctx.getStart().getType() == SearchParser.STRING_LITERAL) {
            return term.substring(1, term.length() - 1)
                       .replace("\\\"", "\"");
        }

        if (ctx.getStart().getType() == SearchParser.TERM) {
            return term.replaceAll("\\\\([=!~()])", "$1");
        }

        return term;
    }

    @NullMarked
    private static final class SearchRegularExpressionValidator extends SearchBaseVisitor<Boolean> {

        private final boolean searchBarRegex;

        private SearchRegularExpressionValidator(EnumSet<SearchFlags> searchFlags) {
            this.searchBarRegex = searchFlags.contains(SearchFlags.REGULAR_EXPRESSION);
        }

        @Override
        public Boolean visitStart(SearchParser.StartContext ctx) {
            if (ctx.andExpression() == null) {
                return true;
            }
            return visit(ctx.andExpression());
        }

        @Override
        public Boolean visitImplicitAndExpression(SearchParser.ImplicitAndExpressionContext ctx) {
            return ctx.expression().stream().allMatch(expression -> visit(expression));
        }

        @Override
        public Boolean visitParenExpression(SearchParser.ParenExpressionContext ctx) {
            return visit(ctx.andExpression());
        }

        @Override
        public Boolean visitNegatedExpression(SearchParser.NegatedExpressionContext ctx) {
            return visit(ctx.expression());
        }

        @Override
        public Boolean visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
            return visit(ctx.left) && visit(ctx.right);
        }

        @Override
        public Boolean visitComparisonExpression(SearchParser.ComparisonExpressionContext ctx) {
            return visit(ctx.comparison());
        }

        @Override
        public Boolean visitComparison(SearchParser.ComparisonContext ctx) {
            if (!isRegexComparison(ctx)) {
                return true;
            }

            String regularExpression = unescapeSearchValue(ctx.searchValue());
            try {
                Pattern.compile(regularExpression);
                return true;
            } catch (PatternSyntaxException e) {
                LOGGER.debug("Invalid regular expression in search query: {}", regularExpression, e);
                return false;
            }
        }

        private boolean isRegexComparison(SearchParser.ComparisonContext ctx) {
            if (ctx.FIELD() == null) {
                return searchBarRegex;
            }

            int operator = ctx.operator().getStart().getType();
            return switch (operator) {
                case SearchParser.REQUAL,
                     SearchParser.CREEQUAL,
                     SearchParser.NREQUAL,
                     SearchParser.NCREEQUAL ->
                        true;
                default ->
                        false;
            };
        }
    }
}
