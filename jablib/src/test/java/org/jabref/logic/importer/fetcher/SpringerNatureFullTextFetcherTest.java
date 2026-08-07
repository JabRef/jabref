package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class SpringerNatureFullTextFetcherTest {

    private final ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
    private SpringerNatureFullTextFetcher finder;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        Optional<String> apiKey = Optional.of(new BuildInfo().springerNatureAPIKey);
        when(importerPreferences.getApiKey(SpringerNatureFullTextFetcher.FETCHER_NAME)).thenReturn(apiKey);
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        finder = new SpringerNatureFullTextFetcher(importerPreferences);
        entry = new BibEntry();
    }

    @Test
    void doiNotPresent() throws IOException {
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @DisabledOnCIServer("Disable on CI Server to not hit the API call limit")
    @Test
    void findByDOI() throws IOException {
        entry.setField(StandardField.DOI, "10.1186/s13677-015-0042-8");
        assertEquals(
                Optional.of(URLUtil.create("http://link.springer.com/content/pdf/10.1186/s13677-015-0042-8.pdf")),
                finder.findFullText(entry));
    }

    @DisabledOnCIServer("Disable on CI Server to not hit the API call limit")
    @Test
    void notFoundByDOI() throws IOException {
        entry.setField(StandardField.DOI, "10.1186/unknown-doi");

        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void entityWithoutDoi() throws IOException {
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void trustLevel() {
        assertEquals(TrustLevel.PUBLISHER, finder.getTrustLevel());
    }
}
