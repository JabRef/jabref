package org.jabref.model.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.QueryTermExtractor;
import org.apache.lucene.search.highlight.WeightedTerm;

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

    private final String query;
    private final EnumSet<SearchFlags> searchFlags;

    private Query parsedQuery;
    private String parseError;
    private SearchResults searchResults;

    public SearchQuery(String query, EnumSet<SearchFlags> searchFlags) {
        this.query = Objects.requireNonNull(query);
        this.searchFlags = searchFlags;

        Map<String, Float> boosts = new HashMap<>();
        Map<String, Analyzer> fieldAnalyzers = new HashMap<>();

        if (searchFlags.contains(SearchFlags.FULLTEXT)) {
            boosts.put(SearchFieldConstants.DEFAULT_FIELD.toString(), 4F);
            SearchFieldConstants.PDF_FIELDS.forEach(field -> {
                boosts.put(field, 1F);
                fieldAnalyzers.put(field, SearchFieldConstants.LINKED_FILES_ANALYZER);
            });
        } else {
            boosts.put(SearchFieldConstants.DEFAULT_FIELD.toString(), 1F);
        }

        String[] fieldsToSearchArray = new String[boosts.size()];
        boosts.keySet().toArray(fieldsToSearchArray);

        PerFieldAnalyzerWrapper analyzerWrapper = new PerFieldAnalyzerWrapper(SearchFieldConstants.LATEX_AWARE_ANALYZER, fieldAnalyzers);
        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(fieldsToSearchArray, analyzerWrapper, boosts);
        queryParser.setAllowLeadingWildcard(true);

        try {
            parsedQuery = queryParser.parse(query);
            parseError = null;
        } catch (Exception e) {
            parsedQuery = null;
            parseError = e.getMessage();
        }
    }

    public String getSearchExpression() {
        return query;
    }

    public SearchResults getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(SearchResults searchResults) {
        this.searchResults = searchResults;
    }

    @Override
    public String toString() {
        return query;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SearchQuery that)) {
            return false;
        }
        return Objects.equals(query, that.query)
                && Objects.equals(searchFlags, that.searchFlags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, searchFlags);
    }

    public boolean isValid() {
        return parseError == null;
    }

    public Query getParsedQuery() {
        return parsedQuery;
    }

    public EnumSet<SearchFlags> getSearchFlags() {
        return searchFlags;
    }

    /**
     * Returns a list of words this query searches for. The returned strings can be a regular expression.
     */
    public List<String> getSearchWords() {
        if (searchFlags.contains(SearchFlags.REGULAR_EXPRESSION)) {
            return Collections.singletonList(query);
        }
        if (!isValid()) {
            return List.of();
        }
        return Arrays.stream(QueryTermExtractor.getTerms(parsedQuery))
                     .map(WeightedTerm::getTerm)
                     .toList();
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
