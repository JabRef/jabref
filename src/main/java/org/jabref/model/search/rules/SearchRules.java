package org.jabref.model.search.rules;

import java.util.EnumSet;
import java.util.regex.Pattern;

public class SearchRules {

    private static final Pattern SIMPLE_EXPRESSION = Pattern.compile("[^\\p{Punct}]*");

    private SearchRules() {
    }

    /**
     * Returns the appropriate search rule that fits best to the given parameter.
     */
    public static SearchRule getSearchRuleByQuery(String query, EnumSet<SearchFlags> searchFlags) {
        if (isSimpleQuery(query)) {
            return new ContainBasedSearchRule(searchFlags);
        }

        // this searches specified fields if specified,
        // and all fields otherwise
        SearchRule searchExpression = new GrammarBasedSearchRule(searchFlags);
        if (searchExpression.validateSearchStrings(query)) {
            return searchExpression;
        } else {
            return getSearchRule(searchFlags);
        }
    }

    private static boolean isSimpleQuery(String query) {
        return SIMPLE_EXPRESSION.matcher(query).matches();
    }

    static SearchRule getSearchRule(EnumSet<SearchFlags> searchFlags) {
        if (searchFlags.contains(SearchFlags.REGULAR_EXPRESSION)) {
            return new RegexBasedSearchRule(searchFlags);
        } else {
            return new ContainBasedSearchRule(searchFlags);
        }
    }

    public enum SearchFlags {
        CASE_SENSITIVE, REGULAR_EXPRESSION, FULLTEXT, KEEP_SEARCH_STRING;
    }
}
