package org.jabref.model.search.query;

public enum SearchTermFlag {
    EXACT_MATCH, INEXACT_MATCH, REGULAR_EXPRESSION, // mutually exclusive
    CASE_SENSITIVE, CASE_INSENSITIVE, // mutually exclusive
    NEGATION,
}
