package org.jabref.logic.importer;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.Keyword;
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
    void normalizeKeywordsUsesConfiguredInputDelimiters() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.KEYWORDS, "keywordOne; keywordTwo# keywordThree");

        KeywordImportNormalizer.normalizeKeywords(entry, new BibEntryPreferences(',', ";#"));

        assertEquals("keywordOne, keywordTwo, keywordThree", entry.getField(StandardField.KEYWORDS).orElseThrow());
    }

    @Test
    void normalizeKeywordsCanInferOneDelimiterByPriority() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.KEYWORDS, "keywordOne, keywordTwo; keywordThree");

        KeywordImportNormalizer.normalizeKeywords(entry,
                new BibEntryPreferences(',', ";,", BibEntryPreferences.ImportDelimiterParsingStrategy.INFER_DELIMITER_BY_PRIORITY));

        assertEquals(List.of("keywordOne, keywordTwo", "keywordThree"), entry.getKeywords(',').stream().map(Keyword::toString).toList());
    }

    @Test
    void normalizeKeywordsDoesNotSplitConfiguredDelimitersInsideBraces() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.KEYWORDS, "test1; {2,1}; test3");

        KeywordImportNormalizer.normalizeKeywords(entry, new BibEntryPreferences(',', ";,"));

        assertEquals(List.of("test1", "{2,1}", "test3"), entry.getKeywords(',').stream().map(Keyword::toString).toList());
    }
}
