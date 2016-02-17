package net.sf.jabref.logic.search;

import net.sf.jabref.logic.search.describer.SearchDescriber;
import net.sf.jabref.logic.search.describer.SearchDescribers;
import net.sf.jabref.logic.search.rules.ContainBasedSearchRule;
import net.sf.jabref.logic.search.rules.GrammarBasedSearchRule;
import net.sf.jabref.model.entry.BibEntry;

public class SearchQuery {

    private final String query;
    private final boolean caseSensitive;
    private final boolean regularExpression;
    private final SearchRule rule;
    private final String description;

    public SearchQuery(String query, boolean caseSensitive, boolean regularExpression) {
        this.query = query;
        this.caseSensitive = caseSensitive;
        this.regularExpression = regularExpression;
        this.rule = getSearchRule();
        this.description = getSearchDescriber().getDescription();
    }

    @Override
    public String toString() {
        return String.format("\"%s\" (%s, %s)", getQuery(), getCaseSensitiveDescription(), getRegularExpressionDescription());
    }

    public boolean isMatch(BibEntry entry) {
        return this.getRule().applyRule(getQuery(), entry);
    }

    public boolean isValidQuery() {
        return this.getRule().validateSearchStrings(getQuery());
    }

    public boolean isContainsBasedSearch() {
        return this.getRule() instanceof ContainBasedSearchRule;
    }

    private SearchRule getSearchRule() {
        return SearchRules.getSearchRuleByQuery(getQuery(), isCaseSensitive(), isRegularExpression());
    }

    private SearchDescriber getSearchDescriber() {
        return SearchDescribers.getSearchDescriberFor(getSearchRule(), getQuery());
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

    public boolean isGrammarBasedSearch() {
        return this.getRule() instanceof GrammarBasedSearchRule;
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

    public String getDescription() {
        return description;
    }

    public SearchRule getRule() {
        return rule;
    }
}
