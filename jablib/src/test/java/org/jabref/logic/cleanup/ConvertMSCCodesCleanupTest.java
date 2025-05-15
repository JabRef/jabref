package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ConvertMSCCodesCleanupTest {

    private CleanupWorker worker;

    @BeforeEach
    void setUp() {
        worker = new CleanupWorker(
                mock(BibDatabaseContext.class),
                mock(FilePreferences.class),
                mock(TimestampPreferences.class),
                mock(BibEntryPreferences.class));
    }

    @Test
    void cleanupConvertsValidMSCCode() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CONVERT_MSC_CODES);
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "03E72");

        worker.cleanup(preset, entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertEquals("Theory of fuzzy sets - etc.", keywords.get());
    }

    @Test
    void cleanupPreservesNonMSCKeywords() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CONVERT_MSC_CODES);
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "03E72, Machine Learning, Artificial Intelligence");

        worker.cleanup(preset, entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertEquals("Theory of fuzzy sets - etc.,Machine Learning,Artificial Intelligence", keywords.get());
    }

    @Test
    void cleanupHandlesInvalidMSCCode() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CONVERT_MSC_CODES);
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "99Z99, Machine Learning");

        worker.cleanup(preset, entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertEquals("99Z99, Machine Learning", keywords.get());
    }

    @Test
    void cleanupHandlesNoKeywordsField() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CONVERT_MSC_CODES);
        BibEntry entry = new BibEntry();

        worker.cleanup(preset, entry);

        assertEquals(Optional.empty(), entry.getField(StandardField.KEYWORDS));
    }

    @Test
    void cleanupHandlesMultipleMSCCodes() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CONVERT_MSC_CODES);
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "03E72, 68T01");

        worker.cleanup(preset, entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertEquals("Theory of fuzzy sets - etc.,General topics in artificial intelligence", keywords.get());
    }
}
