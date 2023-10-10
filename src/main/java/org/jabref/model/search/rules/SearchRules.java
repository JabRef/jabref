package org.jabref.model.search.rules;

/**
 * This is a factory to instantiate the matching SearchRule implementation matching a given query
 */
public class SearchRules {

    public enum SearchFlags {
        REGULAR_EXPRESSION, FULLTEXT, KEEP_SEARCH_STRING, FILTERING_SEARCH, SORT_BY_SCORE;
    }
}
