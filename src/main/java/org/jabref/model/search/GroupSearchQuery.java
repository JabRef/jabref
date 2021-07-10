package org.jabref.model.search;

import java.util.EnumSet;
import java.util.Objects;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.rules.SearchRule;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.search.rules.SearchRules.SearchFlags;

public class GroupSearchQuery implements SearchMatcher {

    private final String query;
    private final EnumSet<SearchFlags> searchFlags;
    private final SearchRule rule;

    public GroupSearchQuery(String query, EnumSet<SearchFlags> searchFlags) {
        this.query = Objects.requireNonNull(query);
        this.searchFlags = searchFlags;
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
        return SearchRules.getSearchRuleByQuery(query, searchFlags);
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

    public SearchRule getRule() {
        return rule;
    }

    public String getSearchExpression() {
        return query;
    }

    public EnumSet<SearchFlags> getSearchFlags() {
        return searchFlags;
    }
}
