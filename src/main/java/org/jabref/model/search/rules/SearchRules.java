package org.jabref.model.search.rules;

import org.jabref.model.strings.StringUtil;

import java.util.regex.Pattern;

public class SearchRules {

    private SearchRules() {
    }

    private static final Pattern simpleExpression = Pattern.compile("(\\w|\\d|\\s)*");

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
        return StringUtil.isBlank(query) || simpleExpression.matcher(query).matches();
    }

    private static SearchRule getSearchRule(boolean caseSensitive, boolean regex) {
        if (regex) {
            return new RegexBasedSearchRule(caseSensitive);
        } else {
            return new ContainBasedSearchRule(caseSensitive);
        }
    }

}
