package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.ImporterPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests method not requiring network access of OpenAlex fetcher.
class OpenAlexTest {

    private OpenAlex fetcher;

    @BeforeEach
    void setUp() {
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        fetcher = new OpenAlex(importerPreferences);
    }

    @ParameterizedTest
    @CsvSource({
            "https://openalex.org/works/W4408614692, W4408614692",
            "https://openalex.org/W4408614692, W4408614692",
            "https://openalex.org/works/W4408614692?foo=bar, W4408614692"
    })
    void extractsId(String url, String expected) {
        assertEquals(Optional.of(expected), fetcher.extractOpenAlexId(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com/W4408614692",
            "not-a-url"
    })
    void returnsEmptyForInvalid(String url) {
        assertEquals(Optional.empty(), fetcher.extractOpenAlexId(url));
    }
}
