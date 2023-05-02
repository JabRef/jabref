package org.jabref.logic.importer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.importer.fetcher.AbstractIsbnFetcher;
import org.jabref.logic.importer.fetcher.CiteSeer;
import org.jabref.logic.importer.fetcher.GoogleScholar;
import org.jabref.logic.importer.fetcher.GrobidCitationFetcher;
import org.jabref.logic.importer.fetcher.JstorFetcher;
import org.jabref.logic.importer.fetcher.MrDLibFetcher;
import org.jabref.logic.importer.fetcher.isbntobibtex.DoiToBibtexConverterComIsbnFetcher;
import org.jabref.logic.importer.fetcher.isbntobibtex.EbookDeIsbnFetcher;
import org.jabref.logic.importer.fetcher.isbntobibtex.OpenLibraryIsbnFetcher;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.FilePreferences;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class WebFetchersTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebFetchersTest.class);
    private static final Set<String> IGNORED_INACCESSIBLE_FETCHERS = Set.of("ArXivFetcher$ArXiv");

    private ImportFormatPreferences importFormatPreferences;
    private ImporterPreferences importerPreferences;
    private final ClassGraph classGraph = new ClassGraph().enableAllInfo().acceptPackages("org.jabref");

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        importerPreferences = mock(ImporterPreferences.class);
    }

    private Set<Class<?>> getIgnoredInaccessibleClasses() {
        return IGNORED_INACCESSIBLE_FETCHERS.stream()
                     .map(className -> "org.jabref.logic.importer.fetcher." + className)
                     .map(classPath -> {
                         try {
                             return Class.forName(classPath);
                         } catch (ClassNotFoundException e) {
                             LOGGER.error("Some of the ignored classes were not found", e);
                             return null;
                         }
                     }).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @Test
    void getIdBasedFetchersReturnsAllFetcherDerivingFromIdBasedFetcher() {
        Set<IdBasedFetcher> idFetchers = WebFetchers.getIdBasedFetchers(importFormatPreferences, importerPreferences);

        try (ScanResult scanResult = classGraph.scan()) {
            ClassInfoList controlClasses = scanResult.getClassesImplementing(IdBasedFetcher.class.getCanonicalName());
            Set<Class<?>> expected = new HashSet<>(controlClasses.loadClasses());

            // Some classes implement IdBasedFetcher, but are only accessible to other fetcher, so ignore them
            expected.removeAll(getIgnoredInaccessibleClasses());

            expected.remove(AbstractIsbnFetcher.class);
            expected.remove(IdBasedParserFetcher.class);

            // Remove special ISBN fetcher since we don't want to expose them to the user
            expected.remove(OpenLibraryIsbnFetcher.class);
            expected.remove(EbookDeIsbnFetcher.class);
            expected.remove(DoiToBibtexConverterComIsbnFetcher.class);

            // Remove the following, because they don't work at the moment
            expected.remove(JstorFetcher.class);
            expected.remove(GoogleScholar.class);

            assertEquals(expected, getClasses(idFetchers));
        }
    }

    @Test
    void getEntryBasedFetchersReturnsAllFetcherDerivingFromEntryBasedFetcher() {
        Set<EntryBasedFetcher> idFetchers = WebFetchers.getEntryBasedFetchers(
                mock(ImporterPreferences.class),
                importFormatPreferences,
                mock(FilePreferences.class),
                mock(BibDatabaseContext.class));

        try (ScanResult scanResult = classGraph.scan()) {
            ClassInfoList controlClasses = scanResult.getClassesImplementing(EntryBasedFetcher.class.getCanonicalName());
            Set<Class<?>> expected = new HashSet<>(controlClasses.loadClasses());

            expected.remove(EntryBasedParserFetcher.class);
            expected.remove(MrDLibFetcher.class);
            assertEquals(expected, getClasses(idFetchers));
        }
    }

    @Test
    void getSearchBasedFetchersReturnsAllFetcherDerivingFromSearchBasedFetcher() {
        Set<SearchBasedFetcher> searchBasedFetchers = WebFetchers.getSearchBasedFetchers(importFormatPreferences, importerPreferences);
        try (ScanResult scanResult = classGraph.scan()) {
            ClassInfoList controlClasses = scanResult.getClassesImplementing(SearchBasedFetcher.class.getCanonicalName());
            Set<Class<?>> expected = new HashSet<>(controlClasses.loadClasses());

            // Some classes implement SearchBasedFetcher, but are only accessible to other fetcher, so ignore them
            expected.removeAll(getIgnoredInaccessibleClasses());

            // Remove interfaces
            expected.remove(SearchBasedParserFetcher.class);

            // Remove the following, because they don't work atm
            expected.remove(JstorFetcher.class);
            expected.remove(GoogleScholar.class);
            expected.remove(CiteSeer.class);

            expected.remove(PagedSearchBasedParserFetcher.class);
            expected.remove(PagedSearchBasedFetcher.class);

            // Remove GROBID, because we don't want to show this to the user
            expected.remove(GrobidCitationFetcher.class);

            assertEquals(expected, getClasses(searchBasedFetchers));
        }
    }

    @Test
    void getFullTextFetchersReturnsAllFetcherDerivingFromFullTextFetcher() {
        Set<FulltextFetcher> fullTextFetchers = WebFetchers.getFullTextFetchers(importFormatPreferences, importerPreferences);

        try (ScanResult scanResult = classGraph.scan()) {
            ClassInfoList controlClasses = scanResult.getClassesImplementing(FulltextFetcher.class.getCanonicalName());
            Set<Class<?>> expected = new HashSet<>(controlClasses.loadClasses());

            // Some classes implement FulltextFetcher, but are only accessible to other fetcher, so ignore them
            expected.removeAll(getIgnoredInaccessibleClasses());

            // Remove the following, because they don't work atm
            expected.remove(JstorFetcher.class);
            expected.remove(GoogleScholar.class);

            assertEquals(expected, getClasses(fullTextFetchers));
        }
    }

    @Test
    void getIdFetchersReturnsAllFetcherDerivingFromIdFetcher() {
        Set<IdFetcher<?>> idFetchers = WebFetchers.getIdFetchers(importFormatPreferences);

        try (ScanResult scanResult = classGraph.scan()) {
            ClassInfoList controlClasses = scanResult.getClassesImplementing(IdFetcher.class.getCanonicalName());
            Set<Class<?>> expected = new HashSet<>(controlClasses.loadClasses());

            // Some classes implement IdFetcher, but are only accessible to other fetcher, so ignore them
            expected.removeAll(getIgnoredInaccessibleClasses());

            expected.remove(IdParserFetcher.class);
            // Remove the following, because they don't work at the moment
            expected.remove(GoogleScholar.class);

            assertEquals(expected, getClasses(idFetchers));
        }
    }

    private Set<? extends Class<?>> getClasses(Collection<?> objects) {
        return objects.stream().map(Object::getClass).collect(Collectors.toSet());
    }
}
