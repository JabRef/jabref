package org.jabref.logic.search;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchMatcher;
import org.jabref.model.search.rules.ContainBasedSearchRule;
import org.jabref.model.search.rules.GrammarBasedSearchRule;
import org.jabref.model.search.rules.SearchRule;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.search.rules.SentenceAnalyzer;

public class SearchQuery implements SearchMatcher {

    private final String query;
    private final boolean caseSensitive;
    private final boolean regularExpression;
    private final SearchRule rule;

    public SearchQuery(String query, boolean caseSensitive, boolean regularExpression) {
        this.query = Objects.requireNonNull(query);
        this.caseSensitive = caseSensitive;
        this.regularExpression = regularExpression;
        this.rule = SearchRules.getSearchRuleByQuery(query, caseSensitive, regularExpression);
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
        if (isCaseSensitive()) {
            return "case sensitive";
        } else {
            return "case insensitive";
        }
    }

    private String getRegularExpressionDescription() {
        if (isRegularExpression()) {
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
        if (isCaseSensitive()) {
            return Localization.lang("case sensitive");
        } else {
            return Localization.lang("case insensitive");
        }
    }

    private String getLocalizedRegularExpressionDescription() {
        if (isRegularExpression()) {
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

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public boolean isRegularExpression() {
        return regularExpression;
    }

    /**
     * Returns a list of words this query searches for.
     * The returned strings can be a regular expression.
     */
    public List<String> getSearchWords() {
        if (isRegularExpression()) {
            return Collections.singletonList(getQuery());
        } else {
            // Parses the search query for valid words and returns a list these words.
            // For example, "The great Vikinger" will give ["The","great","Vikinger"]
            return (new SentenceAnalyzer(getQuery())).getWords();
        }
    }

    public SearchRule getRule() {
        return rule;
    }
}
