package org.jabref.logic.importer;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.preferences.FetcherApiKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImporterPreferencesTest {

    private static final String FETCHER = "SomeFetcher";

    private static ImporterPreferences withCustomKey(String key) {
        return new ImporterPreferences(
                true,
                false,
                Path.of(""),
                false,
                Set.of(),
                Set.of(new FetcherApiKey(FETCHER, true, key)),
                false,
                List.of(),
                PlainCitationParserChoice.RULE_BASED_GENERAL,
                30,
                Map.of());
    }

    @Test
    void getApiKeyReturnsConfiguredKey() {
        assertEquals(Optional.of("real-key"), withCustomKey("real-key").getApiKey(FETCHER));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n", "   "})
    void getApiKeyTreatsBlankKeyAsAbsent(String blank) {
        // A blank key (e.g. an unsubstituted build secret) must be reported as absent so callers
        // do not send an empty api_key= parameter to the remote service.
        assertEquals(Optional.empty(), withCustomKey(blank).getApiKey(FETCHER));
    }

    @Test
    void getApiKeyReturnsEmptyForUnknownFetcher() {
        assertEquals(Optional.empty(), withCustomKey("real-key").getApiKey("UnknownFetcher"));
    }
}
