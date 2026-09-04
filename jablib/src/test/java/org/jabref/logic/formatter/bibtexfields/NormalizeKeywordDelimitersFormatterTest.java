package org.jabref.logic.formatter.bibtexfields;

import org.jabref.model.entry.BibEntryPreferences;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// [utest->req~save.keywords.normalize-delimiters~1]
class NormalizeKeywordDelimitersFormatterTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "keywordOne; keywordTwo; keywordThree|keywordOne; keywordTwo, keywordThree",
            "keywordOne;keywordTwo;keywordThree|keywordOne;keywordTwo;keywordThree",
            "keywordOne; keywordTwo|keywordOne; keywordTwo",
            "single|single",
            "''|''"
    })
    void formatWithSemicolonSeparator(String expected, String input) {
        NormalizeKeywordDelimitersFormatter formatter = new NormalizeKeywordDelimitersFormatter(new BibEntryPreferences(',', ";,"), ';');
        assertEquals(expected, formatter.format(input));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "keywordOne, keywordTwo, keywordThree|keywordOne; keywordTwo; keywordThree",
            "keywordOne, keywordTwo|keywordOne, keywordTwo"
    })
    void formatFallsBackToPreferenceSeparator(String expected, String input) {
        NormalizeKeywordDelimitersFormatter formatter = new NormalizeKeywordDelimitersFormatter(new BibEntryPreferences(',', ";,"), null);
        assertEquals(expected, formatter.format(input));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "keywordOne; keywordTwo|keywordOne, keywordTwo"
    })
    void withKeywordSeparatorBindsLibrarySeparator(String expected, String input) {
        NormalizeKeywordDelimitersFormatter formatter = new NormalizeKeywordDelimitersFormatter(new BibEntryPreferences(',', ";,"), null);
        assertEquals(expected, formatter.withKeywordSeparator(';').format(input));
    }

    @Test
    void formatKeepsFieldsUsingLibrarySeparatorExcludedFromImportDelimiters() {
        NormalizeKeywordDelimitersFormatter formatter = new NormalizeKeywordDelimitersFormatter(new BibEntryPreferences(',', ","), ';');
        assertEquals("keywordOne; keywordTwo", formatter.format("keywordOne; keywordTwo"));
    }
}
