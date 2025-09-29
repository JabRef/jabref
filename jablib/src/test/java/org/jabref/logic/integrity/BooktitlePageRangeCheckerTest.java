package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BooktitlePageRangeCheckerTest {
    private final BooktitlePageRangeChecker pageRangeChecker = new BooktitlePageRangeChecker();

    @ParameterizedTest(name = "Page range checker should detect ranges in: \"{0}\"")
    @CsvSource({
            "'Conference pages 45-67', true",           // Standard range
            "'Workshop 123--456', true",                // Double dash
            "'Pages 100 - 200', true",                  // Spaces around dash
            "'pp 50  --  75', true",                    // Multiple spaces
            "'Conference (pages 25-30)', true",         // In parentheses
            "'Multiple ranges 1-5 and 10-15', true",    // Multiple ranges
            "'Large range 1000-2000', true",            // Large numbers
            "'Single page 42-42', true",                // Same page (technically valid)

            "'Conference 2020', false",                 // No page ranges
            "'Workshop page 45', false",                // Single page
            "'Meeting pages', false",                   // No numbers
            "'Conference 45 67', false",                // No dash
            "'Workshop 45_67', false",                  // Wrong separator
            "'Meeting 45/67', false",                   // Wrong separator
            "'Workshop -45', false",                    // No starting number
            "'Meeting 45-', false",                     // No ending number
            "'Conference a-b', false",                  // Not numbers
            "'Workshop 45-abc', false",                 // Mixed number/text
            "'', false",                                // Empty string
            "'   ', false"                              // Whitespace only
    })
    void pageRangeCheckerDetectsRanges(String input, boolean shouldDetect) {
        Optional<String> result = pageRangeChecker.checkValue(input);
        Optional<String> expected = shouldDetect ? Optional.of("Page range found in booktitle") : Optional.empty();

        assertEquals(expected, result);
    }
}
