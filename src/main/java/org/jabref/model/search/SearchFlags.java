package org.jabref.model.search;

/**
 * This is a factory to instantiate the matching SearchRule implementation matching a given query
 */

public enum SearchFlags {
    REGULAR_EXPRESSION, FULLTEXT, KEEP_SEARCH_STRING, FILTERING_SEARCH, SORT_BY_SCORE
}

