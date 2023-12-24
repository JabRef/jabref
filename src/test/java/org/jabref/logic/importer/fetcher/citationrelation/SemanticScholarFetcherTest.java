package org.jabref.logic.importer.fetcher.citationrelation;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.citationrelation.SemanticScholarFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@FetcherTest
public class SemanticScholarFetcherTest {
    private SemanticScholarFetcher fetcher;
    @BeforeEach
    void setUp() {
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
        fetcher = new SemanticScholarFetcher(importerPreferences);
    }

    @Test
    void testSearchCitedBy() {
        BibEntry ourEntry = new BibEntry()
                .withField(StandardField.DOI, "10.1016/j.optcom.2012.12.002");

        Set<String> allEntriesThatCiteOurEntry = fetcher.searchCitedBy(ourEntry)
                                                     .stream().map(BibEntry::getTitle)
                                                     .filter(Optional::isPresent)
                                                     .map(Optional::get)
                                                     .collect(Collectors.toSet());

        Set<String> someOfTheExpectedCiters = Set.of(
                "Extraordinary light absorptance in graphene superlattices.",
                "Long-range Tamm surface plasmons supported by graphene-dielectric metamaterials",
                "Complex band structures of 1D anisotropic graphene photonic crystal"
        );

        assertTrue(allEntriesThatCiteOurEntry.containsAll(someOfTheExpectedCiters));
    }

    @Test
    void testSearchCiting() {
        BibEntry ourEntry = new BibEntry()
                .withField(StandardField.DOI, "10.1016/j.optcom.2012.12.002");

        Set<String> allEntriesThatOurEntryCites = fetcher.searchCiting(ourEntry)
                                                        .stream().map(BibEntry::getTitle)
                                                        .filter(Optional::isPresent)
                                                        .map(Optional::get)
                                                        .collect(Collectors.toSet());

        Set<String> someOfTheExpectedEntriesThatOurEntryCites = Set.of(
                "Terahertz temperature-dependent defect mode in a semiconductor-dielectric photonic crystal",
                "Analytical solution for photonic band-gap crystals using Drude conductivity"
        );

        assertTrue(allEntriesThatOurEntryCites.containsAll(someOfTheExpectedEntriesThatOurEntryCites));
    }
}
