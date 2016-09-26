package net.sf.jabref.model.search.rules;

import net.sf.jabref.model.strings.StringUtil;

public class SearchRules {

    /**
     * Returns the appropriate search rule that fits best to the given parameter.
     */
    public static SearchRule getSearchRuleByQuery(String query, boolean caseSensitive, boolean regex) {
        if (StringUtil.isBlank(query)) {
            return new ContainBasedSearchRule(caseSensitive);
        }

        // this searches specified fields if specified,
        // and all fields otherwise
        SearchRule searchExpression = new GrammarBasedSearchRule(caseSensitive, regex);
        if (searchExpression.validateSearchStrings(query)) {
            return searchExpression;
        } else {
            return getSearchRule(caseSensitive, regex);
        }
    }

    private static SearchRule getSearchRule(boolean caseSensitive, boolean regex) {
        if (regex) {
            return new RegexBasedSearchRule(caseSensitive);
        } else {
            return new ContainBasedSearchRule(caseSensitive);
        }
    }

}
