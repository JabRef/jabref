package org.jabref.model.search.rules;

import java.util.regex.Pattern;

import org.jabref.model.database.BibDatabaseContext;

public class SearchRules {

    private static final Pattern SIMPLE_EXPRESSION = Pattern.compile("[^\\p{Punct}]*");

    private SearchRules() {
    }

    /**
     * Returns the appropriate search rule that fits best to the given parameter.
     */
    public static SearchRule getSearchRuleByQuery(String query, boolean caseSensitive, boolean regex, boolean fulltext, BibDatabaseContext databaseContext) {
        if (isSimpleQuery(query)) {
            return new ContainBasedSearchRule(caseSensitive, fulltext, databaseContext);
        }

        // this searches specified fields if specified,
        // and all fields otherwise
        SearchRule searchExpression = new GrammarBasedSearchRule(caseSensitive, regex, fulltext, databaseContext);
        if (searchExpression.validateSearchStrings(query)) {
            return searchExpression;
        } else {
            return getSearchRule(caseSensitive, regex, fulltext, databaseContext);
        }
    }

    private static boolean isSimpleQuery(String query) {
        return SIMPLE_EXPRESSION.matcher(query).matches();
    }

    static SearchRule getSearchRule(boolean caseSensitive, boolean regex, boolean fulltext, BibDatabaseContext databaseContext) {
        if (regex) {
            return new RegexBasedSearchRule(caseSensitive, fulltext, databaseContext);
        } else {
            return new ContainBasedSearchRule(caseSensitive, fulltext, databaseContext);
        }
    }
}
