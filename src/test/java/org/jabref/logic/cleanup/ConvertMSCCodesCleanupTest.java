package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ConvertMSCCodesCleanupTest {

    private ConvertMSCCodesCleanup worker;

    @BeforeEach
    void setUp() {
        BibEntryPreferences preferences = mock(BibEntryPreferences.class);
        // Simulate default separator
        Mockito.when(preferences.getKeywordSeparator()).thenReturn(',');
        worker = new ConvertMSCCodesCleanup(preferences, true);
    }

    @Test
    void cleanupConvertsValidMSCCode() {
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "03E72");

        worker.cleanup(entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertEquals("Theory of fuzzy sets - etc.", keywords.get());
    }

    @Test
    void cleanupPreservesNonMSCKeywords() {
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "03E72, Machine Learning, Artificial Intelligence");

        worker.cleanup(entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertEquals("Theory of fuzzy sets - etc.,Machine Learning,Artificial Intelligence", keywords.get());
    }

    @Test
    void cleanupHandlesInvalidMSCCode() {
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "99Z99, Machine Learning");

        worker.cleanup(entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertEquals("99Z99, Machine Learning", keywords.get());
    }

    @Test
    void cleanupHandlesNoKeywordsField() {
        BibEntry entry = new BibEntry();

        worker.cleanup(entry);

        assertEquals(Optional.empty(), entry.getField(StandardField.KEYWORDS));
    }

    @Test
    void cleanupHandlesMultipleMSCCodes() {
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "03E72, 68T01");

        worker.cleanup(entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertEquals("Theory of fuzzy sets - etc.,General topics in artificial intelligence", keywords.get());
    }
}
