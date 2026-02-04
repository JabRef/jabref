package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class OpenAlexCitationFetcherTest {

    private static final Optional<String> API_KEY = Optional.of(new BuildInfo().openAlexApiKey);

    private OpenAlex fetcher;

    @BeforeEach
    void setUp() {
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        when(importerPreferences.getApiKey(MedlineFetcher.FETCHER_NAME)).thenReturn(API_KEY);
        fetcher = new OpenAlex(importerPreferences);
    }

    @Test
    void getCitationsWithDoi() throws FetcherException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.DOI, "10.1016/j.jksuci.2024.102118");

        List<BibEntry> citations = fetcher.getCitations(entry);

        assertFalse(citations.isEmpty());
    }
}
