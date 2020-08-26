package org.jabref.logic.importer;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.fetcher.ComplexSearchQuery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryConverterTest {
    QueryConverter converter = new QueryConverter();

    @Test
    public void convertAuthorField() throws Exception {
        ComplexSearchQuery searchQuery = new QueryConverter().convertQueryStringIntoComplexQuery("author:\"Igor Steinmacher\"");
        assertEquals(List.of("\"Igor Steinmacher\""), searchQuery.getAuthors().get());
    }

    @Test
    public void convertDefaultField() throws Exception {
        ComplexSearchQuery searchQuery = new QueryConverter().convertQueryStringIntoComplexQuery("\"default value\"");
        assertEquals(List.of("\"default value\""), searchQuery.getDefaultFieldPhrases().get());
    }

    @Test
    public void convertExplicitDefaultField() throws Exception {
        ComplexSearchQuery searchQuery = new QueryConverter().convertQueryStringIntoComplexQuery("default:\"default value\"");
        assertEquals(List.of("\"default value\""), searchQuery.getDefaultFieldPhrases().get());
    }

    @Test
    public void convertJournalField() throws Exception {
        ComplexSearchQuery searchQuery = new QueryConverter().convertQueryStringIntoComplexQuery("journal:\"Nature\"");
        assertEquals("\"Nature\"", searchQuery.getJournal().get());
    }

    @Test
    public void convertYearField() throws Exception {
        ComplexSearchQuery searchQuery = new QueryConverter().convertQueryStringIntoComplexQuery("year:2015");
        assertEquals(2015, searchQuery.getSingleYear().get());
    }

    @Test
    public void convertYearRangeField() throws Exception {
        ComplexSearchQuery searchQuery = new QueryConverter().convertQueryStringIntoComplexQuery("year-range:2012-2015");
        assertEquals(2012, searchQuery.getFromYear().get());
        assertEquals(2015, searchQuery.getToYear().get());
    }

    @Test
    public void convertMultipleValuesWithTheSameField() throws Exception {
        ComplexSearchQuery searchQuery = new QueryConverter().convertQueryStringIntoComplexQuery("author:\"Igor Steinmacher\" author:\"Christoph Treude\"");
        List<String> sortedAuthors = searchQuery.getAuthors().get();
        Collections.sort(sortedAuthors);
        assertEquals(List.of("\"Christoph Treude\"", "\"Igor Steinmacher\""), sortedAuthors);
    }
}
