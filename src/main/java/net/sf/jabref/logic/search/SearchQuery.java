package net.sf.jabref.logic.search;

import java.util.Objects;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.search.rules.describer.SearchDescribers;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.search.SearchMatcher;
import net.sf.jabref.model.search.rules.ContainBasedSearchRule;
import net.sf.jabref.model.search.rules.GrammarBasedSearchRule;
import net.sf.jabref.model.search.rules.SearchRule;
import net.sf.jabref.model.search.rules.SearchRules;

public class SearchQuery implements SearchMatcher {

    private final String query;
    private final boolean caseSensitive;
    private final boolean regularExpression;
    private final SearchRule rule;
    private final String description;

    public SearchQuery(String query, boolean caseSensitive, boolean regularExpression) {
        this.query = Objects.requireNonNull(query);
        this.caseSensitive = caseSensitive;
        this.regularExpression = regularExpression;
        this.rule = SearchRules.getSearchRuleByQuery(query, caseSensitive, regularExpression);
        this.description = SearchDescribers.getSearchDescriberFor(rule, query).getDescription();
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

    public String getDescription() {
        return description;
    }

}
