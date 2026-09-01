package org.jabref.logic.importer;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.preferences.FetcherApiKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                List.of(),
                PlainCitationParserChoice.RULE_BASED_GENERAL,
                30,
                Map.of());
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            # a real key is returned as-is
            SomeFetcher,    real-key,  real-key

            # a blank key (e.g. an unsubstituted build secret) is treated as absent,
            # so callers never send an empty api_key= parameter
            SomeFetcher,    '',
            SomeFetcher,    '   ',

            # an unknown fetcher has no key
            UnknownFetcher, real-key,
            """)
    void getApiKeyTreatsBlankAndUnknownAsAbsent(String queried, String configured, String expected) {
        assertEquals(Optional.ofNullable(expected), withCustomKey(configured).getApiKey(queried));
    }

    private static ImporterPreferences withApiKeys(Set<FetcherApiKey> keys) {
        return new ImporterPreferences(
                true,
                false,
                Path.of(""),
                false,
                Set.of(),
                keys,
                List.of(),
                PlainCitationParserChoice.RULE_BASED_GENERAL,
                30,
                Map.of());
    }

    @Test
    void getApiKeySkipsCustomKeyWhenUseIsFalse() {
        ImporterPreferences prefs = withApiKeys(Set.of(
                new FetcherApiKey(FETCHER, false, "my-secret", true)));

        assertEquals(Optional.empty(), prefs.getApiKey(FETCHER));
    }

    @Test
    void getApiKeyReturnsCustomKeyWhenUseIsTrue() {
        ImporterPreferences prefs = withApiKeys(Set.of(
                new FetcherApiKey(FETCHER, true, "my-secret", false)));

        assertEquals(Optional.of("my-secret"), prefs.getApiKey(FETCHER));
    }

    @Test
    void getApiKeyReturnsEmptyForUnknownFetcherWithNoDefault() {
        ImporterPreferences prefs = withApiKeys(Set.of());

        assertEquals(Optional.empty(), prefs.getApiKey("CompletelyUnknownFetcher"));
    }

    @Test
    void persistFlagIsRespectedPerFetcher() {
        FetcherApiKey persistedKey = new FetcherApiKey("FetcherA", true, "key-a", true);
        FetcherApiKey nonPersistedKey = new FetcherApiKey("FetcherB", true, "key-b", false);

        ImporterPreferences prefs = withApiKeys(Set.of(persistedKey, nonPersistedKey));

        assertAll(
                () -> assertTrue(prefs.getApiKeys().stream()
                                      .filter(k -> k.getName().equals("FetcherA"))
                                      .findFirst()
                                      .orElseThrow()
                                      .shouldPersist()),
                () -> assertFalse(prefs.getApiKeys().stream()
                                       .filter(k -> k.getName().equals("FetcherB"))
                                       .findFirst()
                                       .orElseThrow()
                                       .shouldPersist())
        );
    }

    @Test
    void getDefaultReturnsKeysWithUseAndPersistFalse() {
        ImporterPreferences defaultPrefs = ImporterPreferences.getDefault();

        assertFalse(defaultPrefs.getApiKeys().isEmpty());
        for (FetcherApiKey key : defaultPrefs.getApiKeys()) {
            assertFalse(key.shouldUse(), "shouldUse must be false for default key: " + key.getName());
            assertFalse(key.shouldPersist(), "shouldPersist must be false for default key: " + key.getName());
        }
    }

    @Test
    void setApiKeysClearsAndReplacesExistingKeys() {
        ImporterPreferences prefs = withApiKeys(Set.of(
                new FetcherApiKey("OldFetcher", true, "old-key", false)));

        Set<FetcherApiKey> newKeys = Set.of(
                new FetcherApiKey("NewFetcher", true, "new-key", true));
        prefs.setApiKeys(newKeys);

        assertAll(
                () -> assertEquals(1, prefs.getApiKeys().size()),
                () -> assertEquals("NewFetcher", prefs.getApiKeys().iterator().next().getName())
        );
    }

    @Test
    void persistFlagPreservedThroughSetApiKeys() {
        ImporterPreferences prefs = withApiKeys(Set.of());

        Set<FetcherApiKey> keys = new HashSet<>();
        keys.add(new FetcherApiKey("FetcherA", true, "key-a", true));
        keys.add(new FetcherApiKey("FetcherB", false, "key-b", false));
        prefs.setApiKeys(keys);

        assertAll(
                prefs.getApiKeys().stream()
                     .map(key -> {
                         if ("FetcherA".equals(key.getName())) {
                             return (org.junit.jupiter.api.function.Executable) () ->
                                     assertTrue(key.shouldPersist(), "FetcherA should persist");
                         } else {
                             return (org.junit.jupiter.api.function.Executable) () ->
                                     assertFalse(key.shouldPersist(), "FetcherB should not persist");
                         }
                     })
                     .toArray(org.junit.jupiter.api.function.Executable[]::new)
        );
    }
}
