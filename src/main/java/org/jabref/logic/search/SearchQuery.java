package org.jabref.logic.search;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchMatcher;
import org.jabref.model.search.rules.ContainBasedSearchRule;
import org.jabref.model.search.rules.GrammarBasedSearchRule;
import org.jabref.model.search.rules.SearchRule;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.search.rules.SentenceAnalyzer;

public class SearchQuery implements SearchMatcher {

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
    private EnumSet<SearchRules.SearchFlags> searchFlags;
    private final SearchRule rule;

    public SearchQuery(String query, EnumSet<SearchRules.SearchFlags> searchFlags) {
        this.query = Objects.requireNonNull(query);
        this.searchFlags = searchFlags;
        this.rule = SearchRules.getSearchRuleByQuery(query, searchFlags);
    }

    @Override
    public String toString() {
        return String.format("\"%s\" (%s, %s)", getQuery(), getCaseSensitiveDescription(), getRegularExpressionDescription());
    }

    @Override
    public boolean isMatch(BibEntry entry) {
        return rule.applyRule(getQuery(), entry);
    }

    public boolean isValid() {
        return rule.validateSearchStrings(getQuery());
    }

    public boolean isContainsBasedSearch() {
        return rule instanceof ContainBasedSearchRule;
    }

    private String getCaseSensitiveDescription() {
        if (searchFlags.contains(SearchRules.SearchFlags.CASE_SENSITIVE)) {
            return "case sensitive";
        } else {
            return "case insensitive";
        }
    }

    private String getRegularExpressionDescription() {
        if (searchFlags.contains(SearchRules.SearchFlags.REGULAR_EXPRESSION)) {
            return "regular expression";
        } else {
            return "plain text";
        }
    }

    public String localize() {
        return String.format("\"%s\" (%s, %s)",
                getQuery(),
                getLocalizedCaseSensitiveDescription(),
                getLocalizedRegularExpressionDescription());
    }

    private String getLocalizedCaseSensitiveDescription() {
        if (searchFlags.contains(SearchRules.SearchFlags.CASE_SENSITIVE)) {
            return Localization.lang("case sensitive");
        } else {
            return Localization.lang("case insensitive");
        }
    }

    private String getLocalizedRegularExpressionDescription() {
        if (searchFlags.contains(SearchRules.SearchFlags.REGULAR_EXPRESSION)) {
            return Localization.lang("regular expression");
        } else {
            return Localization.lang("plain text");
        }
    }

    /**
     * Tests if the query is an advanced search query described as described in the help
     *
     * @return true if the query is an advanced search query
     */
    public boolean isGrammarBasedSearch() {
        return rule instanceof GrammarBasedSearchRule;
    }

    public String getQuery() {
        return query;
    }

    public EnumSet<SearchRules.SearchFlags> getSearchFlags() {
        return searchFlags;
    }

    /**
     * Returns a list of words this query searches for. The returned strings can be a regular expression.
     */
    public List<String> getSearchWords() {
        if (searchFlags.contains(SearchRules.SearchFlags.REGULAR_EXPRESSION)) {
            return Collections.singletonList(getQuery());
        } else {
            // Parses the search query for valid words and returns a list these words.
            // For example, "The great Vikinger" will give ["The","great","Vikinger"]
            return (new SentenceAnalyzer(getQuery())).getWords();
        }
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

        if ((words == null) || words.isEmpty() || words.get(0).isEmpty()) {
            return Optional.empty();
        }

        // compile the words to a regular expression in the form (w1)|(w2)|(w3)
        Stream<String> joiner = words.stream();
        if (!searchFlags.contains(SearchRules.SearchFlags.REGULAR_EXPRESSION)) {
            // Reformat string when we are looking for a literal match
            joiner = joiner.map(escapeMode::format);
        }
        String searchPattern = joiner.collect(Collectors.joining(")|(", "(", ")"));

        if (searchFlags.contains(SearchRules.SearchFlags.CASE_SENSITIVE)) {
            return Optional.of(Pattern.compile(searchPattern));
        } else {
            return Optional.of(Pattern.compile(searchPattern, Pattern.CASE_INSENSITIVE));
        }
    }

    public SearchRule getRule() {
        return rule;
    }
}
