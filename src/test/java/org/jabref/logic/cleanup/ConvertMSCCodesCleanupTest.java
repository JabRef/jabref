package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.KeywordList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ConvertMSCCodesCleanupTest {

    private ConvertMSCCodesCleanup worker;

    @BeforeEach
    void setUp() {
        BibEntryPreferences preferences = mock(BibEntryPreferences.class);
        worker = new ConvertMSCCodesCleanup(preferences, true);
    }

    @Test
    void cleanupConvertsValidMSCCode() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "03E72");  // Using a real MSC code for "Fuzzy set theory"

        worker.cleanup(entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertTrue(keywords.isPresent());
        assertTrue(keywords.get().contains("Theory of fuzzy sets, etc."));
    }

    @Test
    void cleanupPreservesNonMSCKeywords() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "03E72, Machine Learning, Artificial Intelligence");

        worker.cleanup(entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertTrue(keywords.isPresent());
        assertTrue(keywords.get().contains("Theory of fuzzy sets, etc."));
        assertTrue(keywords.get().contains("Machine Learning"));
        assertTrue(keywords.get().contains("Artificial Intelligence"));
    }

    @Test
    void cleanupHandlesEmptyKeywords() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "");

        worker.cleanup(entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertTrue(keywords.isPresent());
        assertEquals("", keywords.get());
    }

    @Test
    void cleanupHandlesNoKeywordsField() {
        BibEntry entry = new BibEntry();

        worker.cleanup(entry);

        assertEquals(Optional.empty(), entry.getField(StandardField.KEYWORDS));
    }

    @Test
    void cleanupHandlesInvalidMSCCode() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "99Z99, Machine Learning");  // Invalid MSC code

        worker.cleanup(entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertTrue(keywords.isPresent());
        assertTrue(keywords.get().contains("99Z99"));  // Invalid code should remain unchanged
        assertTrue(keywords.get().contains("Machine Learning"));
    }

    @Test
    void cleanupHandlesMultipleMSCCodes() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "03E72, 68T01");  // Fuzzy set theory, General topics in artificial intelligence

        worker.cleanup(entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertTrue(keywords.isPresent());
        assertTrue(keywords.get().contains("Theory of fuzzy sets, etc."));
        assertTrue(keywords.get().contains("General topics in artificial intelligence"));
    }
} 