package org.jabref.logic.integrity;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.util.LocationDetector;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BooktitleLocationCheckerTest {
    private static BooktitleLocationChecker booktitleLocationChecker;

    @BeforeAll
    static void setUpLocationChecker() throws JabRefException {
        // Create test location data - major cities and countries for testing
        String testLocationData = """
                paris
                london
                new york
                san francisco
                hong kong
                st petersburg
                al-kharijah
                'abas abad
                los angeles
                berlin
                vienna
                prague
                tokyo
                seoul
                fort st. john
                """;

        LocationDetector detector = LocationDetector.createTestInstance(
                new ByteArrayInputStream(testLocationData.getBytes(StandardCharsets.UTF_8))
        );
        booktitleLocationChecker = new BooktitleLocationChecker(detector);
    }

    @ParameterizedTest
    @CsvSource({
            // Single word locations
            "'Conference in Paris', true",
            "'London Workshop 2020', true",

            // Multi-word locations
            "'Conference in New York', true",
            "'Los Angeles Workshop', true",
            "'San Francisco Meeting', true",
            "'Workshop in Hong Kong', true",
            "'St Petersburg Conference', true",

            // Symbols in locations
            "'Al-Kharijah Conference', true",
            "'''Abas Abad workshop', true",
            "'Fort St. John meeting', true",

            // Multiple locations
            "'Meeting Berlin, Vienna, Prague', true",
            "'Tokyo and Seoul Conference', true",

            // Case-insensitivity check
            "'CONFERENCE IN PARIS', true",
            "'london WORKSHOP', true",
            "'NEW YORK meeting', true",

            // No locations
            "'AI Conference 2020', false",
            "'Information Retrieval Symposium', false",
            "'Human-Computer Interaction Conference', false",

            // Partial location names (shouldn't match due to word boundaries)
            "'Conference in Par', false", // Partial "Paris"
            "'Lond Workshop', false", // Partial "London"

            // Location names embedded in other words
            "'Parisienne Conference', false",
            "'Londonish Workshop', false",

            // Empty and whitespace
            "'', false",
            "'   ', false",

            // Special characters only
            "'Conference !@#$%^&*()', false",
            "'Workshop []{}|;:,.<>?', false",
            "'Meeting +-=_~`', false"
    })
    void locationCheckerDetectsLocations(String input, boolean shouldDetect) {
        Optional<String> result = booktitleLocationChecker.checkValue(input);
        Optional<String> expected = shouldDetect ? Optional.of("Location(s) found in booktitle") : Optional.empty();

        assertEquals(expected, result);
    }
}
