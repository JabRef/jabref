package net.sf.jabref.search;

import net.sf.jabref.search.rules.BasicRegexSearchRule;
import net.sf.jabref.search.rules.BasicSearchRule;
import net.sf.jabref.search.rules.SearchExpression;

public class SearchRules {

    /**
     * Returns the appropriate search rule that fits best to the given parameter.
     *
     * @param query
     * @param caseSensitive
     * @param regex
     * @return
     */
    public static SearchRule getSearchRuleByQuery(String query, boolean caseSensitive, boolean regex) {
        // this searches specified fields if specified,
        // and all fields otherwise
        SearchRule searchExpression = new SearchExpression(caseSensitive, regex);
        if (searchExpression.validateSearchStrings(query)) {
            return searchExpression;
        } else {
            return getSearchRule(caseSensitive, regex);
        }
    }

    private static SearchRule getSearchRule(boolean caseSensitive, boolean regex) {
        if (regex) {
            return new BasicRegexSearchRule(caseSensitive);
        } else {
            return new BasicSearchRule(caseSensitive);
        }
    }

}
