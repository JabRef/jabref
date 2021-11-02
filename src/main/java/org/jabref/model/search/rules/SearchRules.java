package org.jabref.model.search.rules;

import java.util.EnumSet;
import java.util.regex.Pattern;

/**
 * This is a factory to instantiate the matching SearchRule implementation matching a given query
 */
public class SearchRules {

    // In case Lucene keywords are contained, it is not a simple expression any more
    private static final Pattern SIMPLE_EXPRESSION = Pattern.compile("^((?! AND | OR )[^\\p{Punct}])*$");

    // used for checking the syntax of the query
    private static LuceneBasedSearchRule luceneBasedSearchRule = new LuceneBasedSearchRule(EnumSet.noneOf(SearchFlags.class));

    private SearchRules() {
    }

    /**
     * Returns the appropriate search rule that fits best to the given parameter.
     */
    public static SearchRule getSearchRuleByQuery(String query, EnumSet<SearchFlags> searchFlags) {
        if (isSimpleQuery(query)) {
            return new ContainsBasedSearchRule(searchFlags);
        }

        if (luceneBasedSearchRule.validateSearchStrings(query)) {
            return new LuceneBasedSearchRule(searchFlags);
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
            return new ContainsBasedSearchRule(searchFlags);
        }
    }

    public enum SearchFlags {
        CASE_SENSITIVE, REGULAR_EXPRESSION, FULLTEXT, KEEP_SEARCH_STRING;
    }
}
