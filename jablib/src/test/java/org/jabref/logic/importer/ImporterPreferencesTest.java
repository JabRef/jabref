package org.jabref.logic.importer;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.preferences.FetcherApiKey;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
}
