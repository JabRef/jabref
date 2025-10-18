package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BooktitleMonthCheckerTest {
    private final BooktitleMonthChecker monthChecker = new BooktitleMonthChecker();

    @ParameterizedTest
    @CsvSource({
            // Full month names
            "'Conference January 2020', true",
            "'february Workshop', true",
            "'Meeting in March', true",
            "'April Symposium', true",
            "'May Conference', true",
            "'June Workshop', true",
            "'July Meeting', true",
            "'August Event', true",
            "'september Conference', true",
            "'October Workshop', true",
            "'November Meeting', true",
            "'December Symposium', true",

            // Abbreviated months
            "'Conference Jan 2020', true",
            "'Feb Workshop', true",
            "'Meeting in Mar', true",
            "'Apr Symposium', true",
            "'Jul Conference', true",
            "'Aug Workshop', true",
            "'Sep Meeting', true",
            "'Sept Event', true", // Both sep and sept
            "'Oct Conference', true",
            "'Nov Workshop', true",
            "'Dec Meeting', true",

            // Case-insensitive checks
            "'JANUARY Conference', true",
            "'February WORKSHOP', true",
            "'JAN Meeting', true",
            "'Feb SYMPOSIUM', true",

            // Multiple months
            "'January and February Conference', true",
            "'Workshop Jan-Feb 2020', true",

            // Non-detection cases
            "'Workshop on AI', false",           // No month
            "'mayonnaise Conference', false",    // 'may' embedded in word
            "'january2020', false",              // No word boundary with number
            "'februaryWorkshop', false",         // No word boundary with text
            "'Conference janua', false",         // Partial month
            "'', false",                         // Empty string
            "'   ', false"                       // Whitespace only
    })
    void monthCheckerDetectsMonths(String input, boolean shouldDetect) {
        Optional<String> result = monthChecker.checkValue(input);
        Optional<String> expected = shouldDetect ? Optional.of("Month found in booktitle") : Optional.empty();

        assertEquals(expected, result);
    }
}
