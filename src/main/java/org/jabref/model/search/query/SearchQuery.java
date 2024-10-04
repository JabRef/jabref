package org.jabref.model.search.query;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.search.query.SearchQueryConversion;
import org.jabref.model.search.SearchFlags;

public class SearchQuery {
    /**
     * The mode of escaping special characters in regular expressions
     */
    private enum EscapeMode {
        /**
         * using \Q and \E marks
         */
        JAVA {
            @Override
            String format(String regex) {
                return Pattern.quote(regex);
            }
        },
        /**
         * escaping all javascript regex special characters separately
         */
        JAVASCRIPT {
            @Override
            String format(String regex) {
                return JAVASCRIPT_ESCAPED_CHARS_PATTERN.matcher(regex).replaceAll("\\\\$0");
            }
        };

        /**
         * Regex pattern for escaping special characters in javascript regular expressions
         */
        private static final Pattern JAVASCRIPT_ESCAPED_CHARS_PATTERN = Pattern.compile("[.*+?^${}()|\\[\\]\\\\/]");

        /**
         * Attempt to escape all regex special characters.
         *
         * @param regex a string containing a regex expression
         * @return a regex with all special characters escaped
         */
        abstract String format(String regex);
    }

    private final String searchExpression;
    private final EnumSet<SearchFlags> searchFlags;

    private String parseError;
    private String sqlQuery;
    private String luceneQuery;
    private SearchResults searchResults;

    public SearchQuery(String searchExpression, EnumSet<SearchFlags> searchFlags) {
        this.searchExpression = Objects.requireNonNull(searchExpression);
        this.searchFlags = searchFlags;
    }

    public String getSqlQuery(String table) {
        if (sqlQuery == null) {
            sqlQuery = SearchQueryConversion.searchToSql(table, searchExpression);
        }
        return sqlQuery;
    }

    public String getLuceneQuery() {
        if (luceneQuery == null) {
            luceneQuery = SearchQueryConversion.searchToLucene(searchExpression);
        }
        return luceneQuery;
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

    public boolean isValid() {
        return parseError == null;
    }

    public EnumSet<SearchFlags> getSearchFlags() {
        return searchFlags;
    }

    /**
     * Returns a list of words this query searches for. The returned strings can be a regular expression.
     */
    public List<String> getSearchWords() {
        if (searchFlags.contains(SearchFlags.REGULAR_EXPRESSION)) {
            return Collections.singletonList(searchExpression);
        }
        if (!isValid()) {
            return List.of();
        }
        return List.of();
    }

    // Returns a regular expression pattern in the form (w1)|(w2)| ... wi are escaped if no regular expression search is enabled
    public Optional<Pattern> getPatternForWords() {
        return joinWordsToPattern(EscapeMode.JAVA);
    }

    // Returns a regular expression pattern in the form (w1)|(w2)| ... wi are escaped for javascript if no regular expression search is enabled
    public Optional<Pattern> getJavaScriptPatternForWords() {
        return joinWordsToPattern(EscapeMode.JAVASCRIPT);
    }

    /**
     * Returns a regular expression pattern in the form (w1)|(w2)| ... wi are escaped if no regular expression search is enabled
     *
     * @param escapeMode the mode of escaping special characters in wi
     */
    private Optional<Pattern> joinWordsToPattern(EscapeMode escapeMode) {
        List<String> words = getSearchWords();

        if ((words == null) || words.isEmpty() || words.getFirst().isEmpty()) {
            return Optional.empty();
        }

        // compile the words to a regular expression in the form (w1)|(w2)|(w3)
        Stream<String> joiner = words.stream();
        if (!searchFlags.contains(SearchFlags.REGULAR_EXPRESSION)) {
            // Reformat string when we are looking for a literal match
            joiner = joiner.map(escapeMode::format);
        }
        String searchPattern = joiner.collect(Collectors.joining(")|(", "(", ")"));
        return Optional.of(Pattern.compile(searchPattern, Pattern.CASE_INSENSITIVE));
    }
}
