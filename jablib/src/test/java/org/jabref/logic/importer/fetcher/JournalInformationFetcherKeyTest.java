package org.jabref.logic.importer.fetcher;

import java.util.Set;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.preferences.FetcherApiKey;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JournalInformationFetcherKeyTest {

    @Test
    void usesConfiguredOpenAlexApiKey() {
        ImporterPreferences importerPreferences = ImporterPreferences.getDefault();
        importerPreferences.setApiKeys(Set.of(new FetcherApiKey(OpenAlex.FETCHER_NAME, true, "configured API key")));
        JournalInformationFetcher fetcher = new JournalInformationFetcher(importerPreferences);

        assertEquals("https://api.openalex.org/sources/issn:1545-4509?api_key=configured+API+key", fetcher.getOpenAlexUrl("1545-4509", ""));
    }

    @Test
    void doesNotUseDisabledOpenAlexApiKey() {
        ImporterPreferences importerPreferences = ImporterPreferences.getDefault();
        importerPreferences.setApiKeys(Set.of(new FetcherApiKey(OpenAlex.FETCHER_NAME, false, "configured API key")));
        JournalInformationFetcher fetcher = new JournalInformationFetcher(importerPreferences);

        assertEquals("https://api.openalex.org/sources/issn:1545-4509", fetcher.getOpenAlexUrl("1545-4509", ""));
    }
}
