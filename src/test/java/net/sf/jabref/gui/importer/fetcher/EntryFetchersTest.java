package net.sf.jabref.gui.importer.fetcher;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.jabref.logic.importer.EntryBasedFetcher;
import net.sf.jabref.logic.importer.EntryBasedParserFetcher;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.importer.IdBasedParserFetcher;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.fetcher.AbstractIsbnFetcher;
import net.sf.jabref.logic.importer.fetcher.IsbnViaChimboriFetcher;
import net.sf.jabref.logic.importer.fetcher.IsbnViaEbookDeFetcher;

import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class EntryFetchersTest {

    Reflections reflections = new Reflections("net.sf.jabref");
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
        assertEquals(expected, getClasses(idFetchers));
    }

    private Set<? extends Class<?>> getClasses(List<?> objects) {
        return objects.stream().map(Object::getClass).collect(Collectors.toSet());
    }
}
