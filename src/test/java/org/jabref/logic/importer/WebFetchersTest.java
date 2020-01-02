package org.jabref.logic.importer;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.importer.fetcher.ACMPortalFetcher;
import org.jabref.logic.importer.fetcher.AbstractIsbnFetcher;
import org.jabref.logic.importer.fetcher.IsbnViaEbookDeFetcher;
import org.jabref.logic.importer.fetcher.IsbnViaOttoBibFetcher;
import org.jabref.logic.importer.fetcher.MrDLibFetcher;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class WebFetchersTest {

    private ImportFormatPreferences importFormatPreferences;
    private ClassGraph classGraph = new ClassGraph().enableAllInfo().whitelistPackages("org.jabref");

    @BeforeEach
    void setUp() throws Exception {
        importFormatPreferences = mock(ImportFormatPreferences.class);
    }

    @Test
    void getIdBasedFetchersReturnsAllFetcherDerivingFromIdBasedFetcher() throws Exception {
        Set<IdBasedFetcher> idFetchers = WebFetchers.getIdBasedFetchers(importFormatPreferences);

        try (ScanResult scanResult = classGraph.scan()) {

            ClassInfoList controlClasses = scanResult.getClassesImplementing(IdBasedFetcher.class.getCanonicalName());
            Set<Class<?>> expected = controlClasses.loadClasses().stream().collect(Collectors.toSet());

            expected.remove(AbstractIsbnFetcher.class);
            expected.remove(IdBasedParserFetcher.class);

            // Remove special ISBN fetcher since we don't want to expose them to the user
            expected.remove(IsbnViaEbookDeFetcher.class);
            expected.remove(IsbnViaOttoBibFetcher.class);

            // Remove ACM, because it doesn't work currently
            expected.remove(ACMPortalFetcher.class);

            assertEquals(expected, getClasses(idFetchers));
        }
    }

    @Test
    void getEntryBasedFetchersReturnsAllFetcherDerivingFromEntryBasedFetcher() throws Exception {
        Set<EntryBasedFetcher> idFetchers = WebFetchers.getEntryBasedFetchers(importFormatPreferences);

        try (ScanResult scanResult = classGraph.scan()) {
            ClassInfoList controlClasses = scanResult.getClassesImplementing(EntryBasedFetcher.class.getCanonicalName());
            Set<Class<?>> expected = controlClasses.loadClasses().stream().collect(Collectors.toSet());

            expected.remove(EntryBasedParserFetcher.class);
            expected.remove(MrDLibFetcher.class);
            assertEquals(expected, getClasses(idFetchers));
        }
    }

    @Test
    void getSearchBasedFetchersReturnsAllFetcherDerivingFromSearchBasedFetcher() throws Exception {
        Set<SearchBasedFetcher> searchBasedFetchers = WebFetchers.getSearchBasedFetchers(importFormatPreferences);
        try (ScanResult scanResult = classGraph.scan()) {
            ClassInfoList controlClasses = scanResult.getClassesImplementing(SearchBasedFetcher.class.getCanonicalName());
            Set<Class<?>> expected = controlClasses.loadClasses().stream().collect(Collectors.toSet());

            expected.remove(SearchBasedParserFetcher.class);

            // Remove ACM, because it doesn't work currently
            expected.remove(ACMPortalFetcher.class);

            assertEquals(expected, getClasses(searchBasedFetchers));
        }
    }

    @Test
    void getFullTextFetchersReturnsAllFetcherDerivingFromFullTextFetcher() throws Exception {
        Set<FulltextFetcher> fullTextFetchers = WebFetchers.getFullTextFetchers(importFormatPreferences);

        try (ScanResult scanResult = classGraph.scan()) {
            ClassInfoList controlClasses = scanResult.getClassesImplementing(FulltextFetcher.class.getCanonicalName());
            Set<Class<?>> expected = controlClasses.loadClasses().stream().collect(Collectors.toSet());
            assertEquals(expected, getClasses(fullTextFetchers));
        }
    }

    @Test
    void getIdFetchersReturnsAllFetcherDerivingFromIdFetcher() throws Exception {
        Set<IdFetcher> idFetchers = WebFetchers.getIdFetchers(importFormatPreferences);

        try (ScanResult scanResult = classGraph.scan()) {
            ClassInfoList controlClasses = scanResult.getClassesImplementing(IdFetcher.class.getCanonicalName());
            Set<Class<?>> expected = controlClasses.loadClasses().stream().collect(Collectors.toSet());

            expected.remove(IdParserFetcher.class);
            assertEquals(expected, getClasses(idFetchers));
        }
    }

    private Set<? extends Class<?>> getClasses(Collection<?> objects) {
        return objects.stream().map(Object::getClass).collect(Collectors.toSet());
    }
}
