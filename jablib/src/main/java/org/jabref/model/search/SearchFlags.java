package org.jabref.model.search;

public enum SearchFlags {
    EXACT_MATCH, INEXACT_MATCH, REGULAR_EXPRESSION, // mutually exclusive
    CASE_SENSITIVE, CASE_INSENSITIVE, // mutually exclusive
    FULLTEXT,
    NEGATION
}
