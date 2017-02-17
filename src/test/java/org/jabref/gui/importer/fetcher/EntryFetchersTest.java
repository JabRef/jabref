package org.jabref.gui.importer.fetcher;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.EntryBasedParserFetcher;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.AbstractIsbnFetcher;
import org.jabref.logic.importer.fetcher.IsbnViaChimboriFetcher;
import org.jabref.logic.importer.fetcher.IsbnViaEbookDeFetcher;
import org.jabref.logic.importer.fetcher.MrDLibFetcher;

import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class EntryFetchersTest {

    Reflections reflections = new Reflections("org.jabref");
    ImportFormatPreferences importFormatPreferences;

    @Before
    public void setUp() throws Exception {
        importFormatPreferences = mock(ImportFormatPreferences.class);
    }

    @Test
    public void getIdFetchersReturnsAllFetcherDerivingFromIdFetcher() throws Exception {
        List<IdBasedFetcher> idFetchers = EntryFetchers.getIdFetchers(importFormatPreferences);

        Set<Class<? extends IdBasedFetcher>> expected = reflections.getSubTypesOf(IdBasedFetcher.class);
        expected.remove(AbstractIsbnFetcher.class);
        expected.remove(IdBasedParserFetcher.class);
        // Remove special ISBN fetcher since we don't want to expose them to the user
        expected.remove(IsbnViaChimboriFetcher.class);
        expected.remove(IsbnViaEbookDeFetcher.class);
        assertEquals(expected, getClasses(idFetchers));
    }

    @Test
    public void getEntryBasedFetchersReturnsAllFetcherDerivingFromEntryBasedFetcher() throws Exception {
        List<EntryBasedFetcher> idFetchers = EntryFetchers.getEntryBasedFetchers(importFormatPreferences);

        Set<Class<? extends EntryBasedFetcher>> expected = reflections.getSubTypesOf(EntryBasedFetcher.class);
        expected.remove(EntryBasedParserFetcher.class);
        expected.remove(MrDLibFetcher.class);
        assertEquals(expected, getClasses(idFetchers));
    }

    private Set<? extends Class<?>> getClasses(List<?> objects) {
        return objects.stream().map(Object::getClass).collect(Collectors.toSet());
    }
}
