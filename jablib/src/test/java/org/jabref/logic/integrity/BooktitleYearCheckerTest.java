package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BooktitleYearCheckerTest {
    private final BooktitleYearChecker yearChecker = new BooktitleYearChecker();

    @ParameterizedTest
    @CsvSource({
            "'Conference 2020', true",              // Standard year
            "'Workshop 1999', true",                // Edge of range
            "'Meeting 1600', true",                 // Lower bound
            "'Symposium 2099', true",               // Upper bound
            "'Event 1900 and 2010', true",          // Multiple years
            "'Conference 2020 Workshop', true",     // Year in middle
            "'2021 Annual Meeting', true",          // Year at start
            "'ICML 2019, NIPS 2020', true",         // Multiple conferences with years

            "Conference1999, false",                // No word boundary
            "Workshop2020Edition, false",           // Embedded in word
            "Meeting 1599, false",                  // Below range
            "Event 2100, false",                    // Above range
            "Conference 999, false",                // Too short
            "Workshop 12020, false",                // Too long
            "Conference without years, false",      // No years at all
            "'', false",                            // Empty string
            "   , false"                            // Whitespace only
    })
    void yearCheckerDetectsValidYears(String input, boolean shouldDetect) {
        Optional<String> result = yearChecker.checkValue(input);
        Optional<String> expected = shouldDetect ? Optional.of("Year(s) present in booktitle") : Optional.empty();

        assertEquals(expected, result);
    }
}
