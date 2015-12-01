package net.sf.jabref.logic.search;

import net.sf.jabref.logic.search.SearchRule;
import net.sf.jabref.logic.search.SearchRules;
import net.sf.jabref.logic.search.describer.SearchDescriber;
import net.sf.jabref.logic.search.describer.SearchDescribers;
import net.sf.jabref.logic.search.rules.ContainBasedSearchRule;
import net.sf.jabref.logic.search.rules.GrammarBasedSearchRule;
import net.sf.jabref.model.entry.BibtexEntry;

public class SearchQuery {

    public final String query;
    public final boolean caseSensitive;
    public final boolean regularExpression;
    private final SearchRule rule;
    public final String description;

    public SearchQuery(String query, boolean caseSensitive, boolean regularExpression) {
        this.query = query;
        this.caseSensitive = caseSensitive;
        this.regularExpression = regularExpression;
        this.rule = getSearchRule();
        this.description = getSearchDescriber().getDescription();
    }

    @Override
    public String toString() {
        return String.format("\"%s\" (%s, %s)", query, getCaseSensitiveDescription(), getRegularExpressionDescription());
    }

    public boolean isMatch(BibtexEntry entry) {
        return this.rule.applyRule(query, entry);
    }

    public boolean isValidQuery() {
        return this.rule.validateSearchStrings(query);
    }

    public boolean isContainsBasedSearch() {
        return this.rule instanceof ContainBasedSearchRule;
    }

    private SearchRule getSearchRule() {
        return SearchRules.getSearchRuleByQuery(query, caseSensitive, regularExpression);
    }

    private SearchDescriber getSearchDescriber() {
        return SearchDescribers.getSearchDescriberFor(getSearchRule(), query);
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

    public boolean isGrammarBasedSearch() {
        return this.rule instanceof GrammarBasedSearchRule;
    }
}
