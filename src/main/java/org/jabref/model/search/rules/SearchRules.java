package org.jabref.model.search.rules;

import java.util.regex.Pattern;

import org.jabref.model.strings.StringUtil;

public class SearchRules {


    private static final Pattern SIMPLE_EXPRESSION = Pattern.compile("[^\\p{Punct}]*");

    private SearchRules() {
    }

    /**
     * Returns the appropriate search rule that fits best to the given parameter.
     */
    public static SearchRule getSearchRuleByQuery(String query, boolean caseSensitive, boolean regex) {
        if (isSimpleQuery(query)) {
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

    private static boolean isSimpleQuery(String query) {
        return StringUtil.isBlank(query) || SIMPLE_EXPRESSION.matcher(query).matches();
    }

    private static SearchRule getSearchRule(boolean caseSensitive, boolean regex) {
        if (regex) {
            return new RegexBasedSearchRule(caseSensitive);
        } else {
            return new ContainBasedSearchRule(caseSensitive);
        }
    }

}
