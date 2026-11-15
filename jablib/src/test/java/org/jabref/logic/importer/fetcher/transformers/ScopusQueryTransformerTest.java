package org.jabref.logic.importer.fetcher.transformers;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.logic.importer.fetcher.ComplexSearchQuery;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.search.query.SearchQueryNode;

import org.apache.lucene.index.Term;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
        SearchQueryNode queryNode = new SearchQueryNode(Optional.empty(), "machine");
        Optional<String> result = transformer.transformSearchQuery(queryNode);

        assertEquals("TITLE-ABS-KEY-AUTH(machine)", result.get());
    }

    @Test
    void singleWordTermTransformsWithoutQuotes() {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.empty(), "test");
        Optional<String> result = transformer.transformSearchQuery(queryNode);

        assertEquals("TITLE-ABS-KEY-AUTH(test)", result.get());
    }

    @Test
    void authorFieldTransformsToAuth() {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.of(StandardField.AUTHOR), "Steinmacher");
        Optional<String> result = transformer.transformSearchQuery(queryNode);

        assertTrue(result.get().contains("AUTH(Steinmacher)"));
    }

    @Test
    void titleFieldTransformsToTitle() {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.of(StandardField.TITLE), "machine");
        Optional<String> result = transformer.transformSearchQuery(queryNode);

        assertTrue(result.get().contains("TITLE(machine)"));
    }

    @Test
    void journalFieldTransformsToSrctitle() {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.of(StandardField.JOURNAL), "Nature");
        Optional<String> result = transformer.transformSearchQuery(queryNode);

        assertTrue(result.get().contains("SRCTITLE(Nature)"));
    }

    @Test
    void yearFieldTransformsToPubyear() {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.of(StandardField.YEAR), "2020");
        Optional<String> result = transformer.transformSearchQuery(queryNode);

        assertEquals("PUBYEAR = 2020", result.get());
    }

    @Test
    void doiFieldTransformsToDoi() {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.of(StandardField.DOI), "10.1000/test");
        Optional<String> result = transformer.transformSearchQuery(queryNode);

        assertTrue(result.get().contains("DOI(10.1000/test)"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"tr", "az"})
    @ResourceLock("Localization.lang")
    @ResourceLock("java.util.Locale.default")
    void uppercaseFieldsAreIndependentOfDefaultLocale(String language) {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag(language));
            SearchQueryNode query = new SearchQueryNode(Optional.of(new UnknownField("TITLE")), "machine");
            assertEquals(Optional.of("TITLE(machine)"), transformer.transformSearchQuery(query));
            SearchQueryNode affiliation = new SearchQueryNode(Optional.of(new UnknownField("AFFILIATION")), "university");
            assertEquals(Optional.of("AFFIL(university)"), transformer.transformSearchQuery(affiliation));
            List<Term> terms = List.of(new Term("TITLE", "machine"));
            assertEquals(List.of("\"machine\""), ComplexSearchQuery.fromTerms(terms).getTitlePhrases());
            assertEquals(List.of("\"machine\""), ComplexSearchQuery.builder().terms(terms).build().getTitlePhrases());
        } finally {
            Locale.setDefault(previous);
        }
    }
}
