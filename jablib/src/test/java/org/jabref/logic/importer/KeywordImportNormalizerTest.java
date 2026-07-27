package org.jabref.logic.importer;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class KeywordImportNormalizerTest {

    @Test
    void parseConfiguredDelimitersFallsBackToDefaultForBlankValues() {
        assertEquals(List.of(';', ','), KeywordImportNormalizer.parseConfiguredDelimiters("   "));
    }

    @Test
    void parseConfiguredDelimitersIgnoresWhitespaceDuplicatesAndReservedCharacters() {
        assertEquals(List.of('|', '#', ':'), KeywordImportNormalizer.parseConfiguredDelimiters(" | # | : > \\\\"));
    }

    @Test
    void normalizeKeywordsUsesConfiguredInputSeparators() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.KEYWORDS, "keywordOne| keywordTwo| keywordThree");

        KeywordImportNormalizer.normalizeKeywords(entry, new BibEntryPreferences(',', "|#"));

        assertEquals("keywordOne, keywordTwo, keywordThree", entry.getField(StandardField.KEYWORDS).orElseThrow());
    }
}
