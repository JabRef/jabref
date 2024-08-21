package org.jabref.logic.importer;

import org.jabref.logic.importer.fetcher.ComplexSearchQuery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryParserTest {
    QueryParser parser = new QueryParser();

    @Test
    void convertAuthorField() {
        ComplexSearchQuery searchQuery = parser.parseQueryStringIntoComplexQuery("author:\"Igor Steinmacher\"").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().author("\"Igor Steinmacher\"").build();
        assertEquals(expectedQuery, searchQuery);
    }

    @Test
    void convertDefaultField() {
        ComplexSearchQuery searchQuery = parser.parseQueryStringIntoComplexQuery("\"default value\"").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().defaultFieldPhrase("\"default value\"").build();
        assertEquals(expectedQuery, searchQuery);
    }

    @Test
    void convertExplicitDefaultField() {
        ComplexSearchQuery searchQuery = parser.parseQueryStringIntoComplexQuery("default:\"default value\"").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().defaultFieldPhrase("\"default value\"").build();
        assertEquals(expectedQuery, searchQuery);
    }

    @Test
    void convertJournalField() {
        ComplexSearchQuery searchQuery = parser.parseQueryStringIntoComplexQuery("journal:Nature").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().journal("\"Nature\"").build();
        assertEquals(expectedQuery, searchQuery);
    }

    @Test
    void convertAlphabeticallyFirstJournalField() {
        ComplexSearchQuery searchQuery = parser.parseQueryStringIntoComplexQuery("journal:Nature journal:\"Complex Networks\"").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().journal("\"Complex Networks\"").build();
        assertEquals(expectedQuery, searchQuery);
    }

    @Test
    void convertYearField() {
        ComplexSearchQuery searchQuery = parser.parseQueryStringIntoComplexQuery("year:2015").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().singleYear(2015).build();
        assertEquals(expectedQuery, searchQuery);
    }

    @Test
    void convertNumericallyFirstYearField() {
        ComplexSearchQuery searchQuery = parser.parseQueryStringIntoComplexQuery("year:2015 year:2014").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().singleYear(2014).build();
        assertEquals(expectedQuery, searchQuery);
    }

    @Test
    void convertYearRangeField() {
        ComplexSearchQuery searchQuery = parser.parseQueryStringIntoComplexQuery("year-range:2012-2015").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().fromYearAndToYear(2012, 2015).build();
        assertEquals(expectedQuery, searchQuery);
    }

    @Test
    void convertMultipleValuesWithTheSameField() {
        ComplexSearchQuery searchQuery = parser.parseQueryStringIntoComplexQuery("author:\"Igor Steinmacher\" author:\"Christoph Treude\"").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().author("\"Igor Steinmacher\"").author("\"Christoph Treude\"").build();
        assertEquals(expectedQuery, searchQuery);
    }
}
