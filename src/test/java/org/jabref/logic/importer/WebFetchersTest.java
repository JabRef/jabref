package org.jabref.logic.importer;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.importer.fetcher.AbstractIsbnFetcher;
import org.jabref.logic.importer.fetcher.IsbnViaChimboriFetcher;
import org.jabref.logic.importer.fetcher.IsbnViaEbookDeFetcher;
import org.jabref.logic.importer.fetcher.MrDLibFetcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class WebFetchersTest {

    Reflections reflections = new Reflections("org.jabref");
    ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setUp() throws Exception {
        importFormatPreferences = mock(ImportFormatPreferences.class);
    }

    @Test
    void getIdBasedFetchersReturnsAllFetcherDerivingFromIdBasedFetcher() throws Exception {
        List<IdBasedFetcher> idFetchers = WebFetchers.getIdBasedFetchers(importFormatPreferences);

        Set<Class<? extends IdBasedFetcher>> expected = reflections.getSubTypesOf(IdBasedFetcher.class);
        expected.remove(AbstractIsbnFetcher.class);
        expected.remove(IdBasedParserFetcher.class);
        // Remove special ISBN fetcher since we don't want to expose them to the user
        expected.remove(IsbnViaChimboriFetcher.class);
        expected.remove(IsbnViaEbookDeFetcher.class);
        assertEquals(expected, getClasses(idFetchers));
    }

    @Test
    void getEntryBasedFetchersReturnsAllFetcherDerivingFromEntryBasedFetcher() throws Exception {
        List<EntryBasedFetcher> idFetchers = WebFetchers.getEntryBasedFetchers(importFormatPreferences);

        Set<Class<? extends EntryBasedFetcher>> expected = reflections.getSubTypesOf(EntryBasedFetcher.class);
        expected.remove(EntryBasedParserFetcher.class);
        expected.remove(MrDLibFetcher.class);
        assertEquals(expected, getClasses(idFetchers));
    }

    @Test
    void getSearchBasedFetchersReturnsAllFetcherDerivingFromSearchBasedFetcher() throws Exception {
        List<SearchBasedFetcher> searchBasedFetchers = WebFetchers.getSearchBasedFetchers(importFormatPreferences);

        Set<Class<? extends SearchBasedFetcher>> expected = reflections.getSubTypesOf(SearchBasedFetcher.class);
        expected.remove(SearchBasedParserFetcher.class);
        assertEquals(expected, getClasses(searchBasedFetchers));
    }

    @Test
    void getFullTextFetchersReturnsAllFetcherDerivingFromFullTextFetcher() throws Exception {
        List<FulltextFetcher> fullTextFetchers = WebFetchers.getFullTextFetchers(importFormatPreferences);

        Set<Class<? extends FulltextFetcher>> expected = reflections.getSubTypesOf(FulltextFetcher.class);
        assertEquals(expected, getClasses(fullTextFetchers));
    }

    @Test
    void getIdFetchersReturnsAllFetcherDerivingFromIdFetcher() throws Exception {
        List<IdFetcher> idFetchers = WebFetchers.getIdFetchers(importFormatPreferences);

        Set<Class<? extends IdFetcher>> expected = reflections.getSubTypesOf(IdFetcher.class);
        expected.remove(IdParserFetcher.class);
        assertEquals(expected, getClasses(idFetchers));
    }

    private Set<? extends Class<?>> getClasses(List<?> objects) {
        return objects.stream().map(Object::getClass).collect(Collectors.toSet());
    }
}
