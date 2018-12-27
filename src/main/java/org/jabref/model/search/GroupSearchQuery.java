package org.jabref.model.search;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.rules.SearchRule;
import org.jabref.model.search.rules.SearchRules;

public class GroupSearchQuery implements SearchMatcher {

    private final String query;
    private final boolean caseSensitive;
    private final boolean regularExpression;
    private final SearchRule rule;

    public GroupSearchQuery(String query, boolean caseSensitive, boolean regularExpression) {
        this.query = Objects.requireNonNull(query);
        this.caseSensitive = caseSensitive;
        this.regularExpression = regularExpression;
        this.rule = Objects.requireNonNull(getSearchRule());
    }

    @Override
    public String toString() {
        return String.format("\"%s\" (%s, %s)", query, getCaseSensitiveDescription(),
                getRegularExpressionDescription());
    }

    @Override
    public boolean isMatch(BibEntry entry) {
        return this.getRule().applyRule(query, entry);
    }

    private SearchRule getSearchRule() {
        return SearchRules.getSearchRuleByQuery(query, caseSensitive, regularExpression);
    }

    private String getCaseSensitiveDescription() {
        if (caseSensitive) {
            return "case sensitive";
        } else {
            return "case insensitive";
        }
    }

    private String getRegularExpressionDescription() {
        if (regularExpression) {
            return "regular expression";
        } else {
            return "plain text";
        }
    }

    public SearchRule getRule() {
        return rule;
    }

    public String getSearchExpression() {
        return query;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public boolean isRegularExpression() {
        return regularExpression;
    }
}
