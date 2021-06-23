package org.jabref.model.search.rules;

import java.util.regex.Pattern;

public class SearchRules {

    private static final Pattern SIMPLE_EXPRESSION = Pattern.compile("[^\\p{Punct}]*");

    private SearchRules() {
    }

    /**
     * Returns the appropriate search rule that fits best to the given parameter.
     */
    public static SearchRule getSearchRuleByQuery(String query, boolean caseSensitive, boolean regex, boolean fulltext) {
        if (isSimpleQuery(query)) {
            return new ContainBasedSearchRule(caseSensitive, fulltext);
        }

        // this searches specified fields if specified,
        // and all fields otherwise
        SearchRule searchExpression = new GrammarBasedSearchRule(caseSensitive, regex, fulltext);
        if (searchExpression.validateSearchStrings(query)) {
            return searchExpression;
        } else {
            return getSearchRule(caseSensitive, regex, fulltext);
        }
    }

    private static boolean isSimpleQuery(String query) {
        return SIMPLE_EXPRESSION.matcher(query).matches();
    }

    static SearchRule getSearchRule(boolean caseSensitive, boolean regex, boolean fulltext) {
        if (regex) {
            return new RegexBasedSearchRule(caseSensitive, fulltext);
        } else {
            return new ContainBasedSearchRule(caseSensitive, fulltext);
        }
    }
}
