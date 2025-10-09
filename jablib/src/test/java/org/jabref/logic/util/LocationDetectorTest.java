package org.jabref.logic.util;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.JabRefException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocationDetectorTest {
    private static LocationDetector locationDetector;

    @BeforeAll
    static void setUpLocationDetector() throws JabRefException {
        // Create test location data - major cities and countries for testing
        String testLocationData = """
                paris
                london
                tokyo
                new york
                san francisco
                ho chi minh city
                rio de janeiro
                singapore
                hong kong
                bangkok
                berlin
                vienna
                prague
                seoul
                los angeles
                madrid
                rome
                al-kharijah
                'abas abad
                fort st. john
                """;

        locationDetector = LocationDetector.createTestInstance(
                new ByteArrayInputStream(testLocationData.getBytes(StandardCharsets.UTF_8))
        );
    }

    @ParameterizedTest(name = "Should detect multiple locations in: \"{0}\" -> {1}")
    @MethodSource("multipleLocationsTestData")
    void detectLocations(String input, Set<String> expectedLocations) {
        assertEquals(expectedLocations, locationDetector.extractLocations(input));
    }

    static Stream<Arguments> multipleLocationsTestData() {
        return Stream.of(
                // Single-word locations
                Arguments.of("Conference in Paris", Set.of("paris")),
                Arguments.of("London Workshop 2020", Set.of("london")),
                Arguments.of("Meeting in Tokyo", Set.of("tokyo")),

                // Multi-word locations
                Arguments.of("Conference in New York", Set.of("new york")),
                Arguments.of("Los Angeles Workshop", Set.of("los angeles")),
                Arguments.of("San Francisco Meeting", Set.of("san francisco")),
                Arguments.of("Ho Chi Minh City Workshop", Set.of("ho chi minh city")),
                Arguments.of("Rio de Janeiro Conference", Set.of("rio de janeiro")),

                // Symbols in locations
                Arguments.of("Al-Kharijah Conference'", Set.of("al-kharijah")),
                Arguments.of("'Abas Abad workshop", Set.of("'abas abad")),
                Arguments.of("Fort St. John Meeting", Set.of("fort st. john")),

                // Case-sensitivity
                Arguments.of("PARIS Conference", Set.of("paris")),
                Arguments.of("london WORKSHOP", Set.of("london")),
                Arguments.of("SAN FRANCISCO conference", Set.of("san francisco")),
                Arguments.of("HONG KONG workshop", Set.of("hong kong")),

                // Multiple locations
                Arguments.of("Conference London and Paris", Set.of("london", "paris")),
                Arguments.of("Workshop in New York and Boston", Set.of("new york")), // Boston not in test data
                Arguments.of("Meeting Berlin, Vienna, Prague", Set.of("berlin", "vienna", "prague")),
                Arguments.of("Tokyo and Seoul Conference", Set.of("tokyo", "seoul")),
                Arguments.of("Workshop Paris London Tokyo", Set.of("paris", "london", "tokyo")),
                Arguments.of("San Francisco and Los Angeles Meeting", Set.of("san francisco", "los angeles")),
                Arguments.of("Conference in Madrid, Barcelona, Rome", Set.of("madrid", "rome")), // Barcelona not in test data
                Arguments.of("Hong Kong Singapore Bangkok Workshop", Set.of("hong kong", "singapore", "bangkok")),

                // No actual locations
                Arguments.of("AI Conference 2020", Set.of()),
                Arguments.of("Machine Learning Workshop", Set.of()),

                // Partial location names
                Arguments.of("Conference in Par", Set.of()), // Partial "Paris"
                Arguments.of("Lond Workshop", Set.of()), // Partial "London"

                // Locations embedded in other words
                Arguments.of("Parisienne Conference", Set.of()),
                Arguments.of("Londonish Workshop", Set.of()),

                // Special characters and symbols
                Arguments.of("Conference !@#$%^&*()", Set.of()),
                Arguments.of("Workshop []{}|;:,.<>?", Set.of()),
                Arguments.of("Meeting +-=_~`", Set.of()),

                // Empty and whitespace inputs
                Arguments.of("", Set.of()), // Empty string
                Arguments.of("   ", Set.of()) // Whitespace only
        );
    }
}
