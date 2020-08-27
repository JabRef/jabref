package org.jabref.logic.importer;

import org.jabref.logic.importer.fetcher.ComplexSearchQuery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryParserTest {
    QueryParser parser = new QueryParser();

    @Test
    public void convertAuthorField() throws Exception {
        ComplexSearchQuery searchQuery = parser.parseQueryStringIntoComplexQuery("author:\"Igor Steinmacher\"").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().author("\"Igor Steinmacher\"").build();
        assertEquals(expectedQuery, searchQuery);
    }

    @Test
    public void convertDefaultField() throws Exception {
        ComplexSearchQuery searchQuery = parser.parseQueryStringIntoComplexQuery("\"default value\"").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().defaultFieldPhrase("\"default value\"").build();
        assertEquals(expectedQuery, searchQuery);
    }

    @Test
    public void convertExplicitDefaultField() throws Exception {
        ComplexSearchQuery searchQuery = parser.parseQueryStringIntoComplexQuery("default:\"default value\"").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().defaultFieldPhrase("\"default value\"").build();
        assertEquals(expectedQuery, searchQuery);
    }

    @Test
    public void convertJournalField() throws Exception {
        ComplexSearchQuery searchQuery = parser.parseQueryStringIntoComplexQuery("journal:\"Nature\"").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().journal("\"Nature\"").build();
        assertEquals(expectedQuery, searchQuery);
    }

    @Test
    public void convertYearField() throws Exception {
        ComplexSearchQuery searchQuery = parser.parseQueryStringIntoComplexQuery("year:2015").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().singleYear(2015).build();
        assertEquals(expectedQuery, searchQuery);
    }

    @Test
    public void convertYearRangeField() throws Exception {
        ComplexSearchQuery searchQuery = parser.parseQueryStringIntoComplexQuery("year-range:2012-2015").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().fromYearAndToYear(2012, 2015).build();
        assertEquals(expectedQuery, searchQuery);
    }

    @Test
    public void convertMultipleValuesWithTheSameField() throws Exception {
        ComplexSearchQuery searchQuery = parser.parseQueryStringIntoComplexQuery("author:\"Igor Steinmacher\" author:\"Christoph Treude\"").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().author("\"Igor Steinmacher\"").author("\"Christoph Treude\"").build();
        assertEquals(expectedQuery, searchQuery);
    }
}
