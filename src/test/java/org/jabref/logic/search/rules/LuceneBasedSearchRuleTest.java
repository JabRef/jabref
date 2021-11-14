package org.jabref.logic.search.rules;

import java.util.EnumSet;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuceneBasedSearchRuleTest {

    private final BibEntry bibEntry = new BibEntry(StandardEntryType.InCollection)
            .withCitationKey("shields01")
            .withField(StandardField.TITLE, "Marine finfish larviculture in Europe")
            .withField(StandardField.YEAR, "2001")
            .withField(StandardField.AUTHOR, "Kevin Shields")
            .withField(StandardField.GROUPS, "included");

    private final LuceneBasedSearchRule luceneBasedSearchRuleCaseSensitive = new LuceneBasedSearchRule(EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE));
    private final LuceneBasedSearchRule luceneBasedSearchRuleCaseInsensitive = new LuceneBasedSearchRule(EnumSet.noneOf(SearchRules.SearchFlags.class));

    @ParameterizedTest
    @ValueSource(strings = {
            "year:2001",
            "year=2001",

            // Current JabRef special feature: sub strings are also matched
            "title:Marine",
            "title=Marine",

            // TODO glob patterns: "title:Marine*"

            "year:2001 title:Marine",
            "year=2001 title=Marine",
            "year=2001 title:Marine",

            "year:2001 AND title:Marine",

            "year:2001 OR title:Marine",

            "Marine",

            "(author = miller or title|keywords = \"finfish\") and not author = brown",

            // RegEx syntax of Lucene
            "/M[a-z]+e/"
    })
    public void findsCaseSensitive(String query) {
        assertTrue(luceneBasedSearchRuleCaseSensitive.validateSearchStrings(query));
        assertTrue(luceneBasedSearchRuleCaseSensitive.applyRule(query, bibEntry));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "year:2002",
            "year=2002",

            // Current JabRef special feature: sub strings are also matched
            "title:Marie",
            "title=Marie",

            // TODO glob patterns: "title:Marine*"

            "year:2002 title:Marine",
            "year=2002 title=Marine",
            "year=2002 title:Marine",

            "year:2002 AND title:Marine",

            "year:2002 OR title:Marie",

            "Marie",

            "/M[0-9]+e/",

            // this tests for grouping (indicated the brackets)
            "(groups=excluded)",

            // this tests for the NOT operator, grouping and Boolean AND
            "NOT(groups=excluded) AND NOT(groups=included)"
    })
    public void notFindsCaseSensitive(String query) {
        assertTrue(luceneBasedSearchRuleCaseSensitive.validateSearchStrings(query));
        assertFalse(luceneBasedSearchRuleCaseSensitive.applyRule(query, bibEntry));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "year:2001",

            "title:Marine",

            "year:2001 title:Marine",

            "year:2001 AND title:Marine",

            "year:2001 OR title:Marine",

            "Marine"
    })
    public void findsCaseInSensitive(String query) {
        assertTrue(luceneBasedSearchRuleCaseInsensitive.validateSearchStrings(query));
        assertTrue(luceneBasedSearchRuleCaseInsensitive.applyRule(query, bibEntry));
    }

}
