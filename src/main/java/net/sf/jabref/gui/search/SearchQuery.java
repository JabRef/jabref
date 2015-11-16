package net.sf.jabref.gui.search;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.search.SearchRule;
import net.sf.jabref.logic.search.SearchRules;
import net.sf.jabref.logic.search.describer.SearchDescriber;
import net.sf.jabref.logic.search.describer.SearchDescribers;

public class SearchQuery {

    public final String query;
    public final boolean caseSensitive;
    public final boolean regularExpression;
    public final SearchRule rule;
    public final String description;

    SearchQuery(String query, boolean caseSensitive, boolean regularExpression) {
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

    private SearchRule getSearchRule() {
        return SearchRules.getSearchRuleByQuery(query, caseSensitive, regularExpression);
    }

    private SearchDescriber getSearchDescriber() {
        return SearchDescribers.getSearchDescriberFor(getSearchRule(), query);
    }

    private String getCaseSensitiveDescription() {
        if (caseSensitive) {
            return Localization.lang("case sensitive");
        } else {
            return Localization.lang("case insensitive");
        }
    }

    private String getRegularExpressionDescription() {
        if (regularExpression) {
            return Localization.lang("regular expression");
        } else {
            return Localization.lang("plain text");
        }
    }

}
