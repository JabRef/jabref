package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.query.SearchQueryNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScopusQueryTransformerTest {

    private ScopusQueryTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new ScopusQueryTransformer();
    }

    @Test
    void simpleTermTransformsToTitleAbsKeyAuth() {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.empty(), "machine learning");
        Optional<String> result = transformer.transformSearchQuery(queryNode);

        assertTrue(result.isPresent());
        assertEquals("TITLE-ABS-KEY-AUTH(\"machine learning\")", result.get());
    }

    @Test
    void singleWordTermTransformsWithoutQuotes() {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.empty(), "test");
        Optional<String> result = transformer.transformSearchQuery(queryNode);

        assertTrue(result.isPresent());
        assertEquals("TITLE-ABS-KEY-AUTH(test)", result.get());
    }

    @Test
    void authorFieldTransformsToAuth() {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.of(StandardField.AUTHOR), "Steinmacher");
        Optional<String> result = transformer.transformSearchQuery(queryNode);

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("AUTH(Steinmacher)"));
    }

    @Test
    void titleFieldTransformsToTitle() {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.of(StandardField.TITLE), "machine learning");
        Optional<String> result = transformer.transformSearchQuery(queryNode);

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("TITLE("));
    }

    @Test
    void journalFieldTransformsToSrctitle() {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.of(StandardField.JOURNAL), "Nature");
        Optional<String> result = transformer.transformSearchQuery(queryNode);

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("SRCTITLE(Nature)"));
    }

    @Test
    void yearFieldTransformsToPubyear() {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.of(StandardField.YEAR), "2020");
        Optional<String> result = transformer.transformSearchQuery(queryNode);

        assertTrue(result.isPresent());
        assertEquals("PUBYEAR = 2020", result.get());
    }

    @Test
    void doiFieldTransformsToDoi() {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.of(StandardField.DOI), "10.1000/test");
        Optional<String> result = transformer.transformSearchQuery(queryNode);

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("DOI(10.1000/test)"));
    }

    @Test
    void logicalOperators() {
        assertEquals(" AND ", transformer.getLogicalAndOperator());
        assertEquals(" OR ", transformer.getLogicalOrOperator());
        assertEquals("AND NOT ", transformer.getLogicalNotOperator());
    }
}
