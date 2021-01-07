package org.jabref.logic.importer;

import org.jabref.logic.importer.fetcher.ComplexSearchQuery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpringerQueryTransformatorTest {
    SpringerQueryTransformator parser = new SpringerQueryTransformator();

    @Test
    public void convertAuthorField() throws Exception {
        String searchQuery = parser.parseQueryStringIntoComplexQuery("author:\"Igor Steinmacher\"").get();
        ComplexSearchQuery expectedQuery = ComplexSearchQuery.builder().author("\"Igor Steinmacher\"").build();
        assertEquals("name:\"Igor Steinmacher\"", searchQuery);
    }

    @Test
    public void convertDefaultField() throws Exception {
        String searchQuery = parser.parseQueryStringIntoComplexQuery("\"default value\"").get();
        assertEquals("\"default value\"", searchQuery);
    }

    @Test
    public void convertExplicitDefaultField() throws Exception {
        String searchQuery = parser.parseQueryStringIntoComplexQuery("default:\"default value\"").get();
        assertEquals("\"default value\"", searchQuery);
    }

    @Test
    public void convertJournalField() throws Exception {
        String searchQuery = parser.parseQueryStringIntoComplexQuery("journal:Nature").get();
        assertEquals("journal:\"Nature\"", searchQuery);
    }

    @Test
    public void convertAlphabeticallyFirstJournalField() throws Exception {
        String searchQuery = parser.parseQueryStringIntoComplexQuery("journal:Nature journal:\"Complex Networks\"").get();
        assertEquals("(journal:\"Nature\" AND journal:\"Complex Networks\")", searchQuery);
    }

    @Test
    public void convertYearField() throws Exception {
        String searchQuery = parser.parseQueryStringIntoComplexQuery("year:2015").get();
        assertEquals("date:2015*", searchQuery);
    }

    @Test
    public void convertNumericallyFirstYearField() throws Exception {
        String searchQuery = parser.parseQueryStringIntoComplexQuery("year:2015 year:2014").get();
        assertEquals("(date:2015* AND date:2014*)", searchQuery);
    }

    @Test
    public void convertYearRangeField() throws Exception {
        String searchQuery = parser.parseQueryStringIntoComplexQuery("year-range:2012-2015").get();
        assertEquals("(date:2012* OR date:2013* OR date:2014* OR date:2015*)", searchQuery);
    }

    @Test
    public void convertMultipleValuesWithTheSameField() throws Exception {
        String searchQuery = parser.parseQueryStringIntoComplexQuery("author:\"Igor Steinmacher\" author:\"Christoph Treude\"").get();
        assertEquals("(name:\"Igor Steinmacher\" AND name:\"Christoph Treude\")", searchQuery);
    }

    @Test
    public void groupedOperations() throws Exception {
        String searchQuery = parser.parseQueryStringIntoComplexQuery("(author:\"Igor Steinmacher\" OR author:\"Christoph Treude\" AND name:\"Christoph Freunde\") AND year:2015").get();
        assertEquals("(name:\"Igor Steinmacher\" OR (name:\"Christoph Treude\" AND name:\"Christoph Freunde\")) AND date:2015*", searchQuery);
    }
}
