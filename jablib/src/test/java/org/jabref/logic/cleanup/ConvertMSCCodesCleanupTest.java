package org.jabref.logic.cleanup;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConvertMSCCodesCleanupTest {

    private CleanupWorker worker;
    private BibEntryPreferences bibEntryPreferences;
    private ConvertMSCCodesCleanup convertMscCleanup;

    @BeforeEach
    void setUp() {
        bibEntryPreferences = mock(BibEntryPreferences.class);
        // Set up the keyword separator that matches what's being used
        when(bibEntryPreferences.getKeywordSeparator()).thenReturn(',');

        worker = new CleanupWorker(
                mock(BibDatabaseContext.class),
                mock(FilePreferences.class),
                mock(TimestampPreferences.class),
                bibEntryPreferences);

        // Create the cleanup job directly for some tests
        convertMscCleanup = new ConvertMSCCodesCleanup(new BibEntryPreferences('.'), true);
    }

    @Test
    void cleanupConvertsValidMSCCode() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CONVERT_MSC_CODES);
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "03E75");

        worker.cleanup(preset, entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertEquals("Applications of set theory", keywords.get());
    }

    @Test
    void cleanupPreservesNonMSCKeywords() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CONVERT_MSC_CODES);
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "03E75, Machine Learning, Artificial Intelligence");

        worker.cleanup(preset, entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertEquals("Applications of set theory,Machine Learning,Artificial Intelligence", keywords.get());
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
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "03E75, 68T01");

        worker.cleanup(preset, entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertEquals("Applications of set theory,General topics in artificial intelligence", keywords.get());
    }

    @Test
    void cleanupReturnsCorrectFieldChanges() {
        ConvertMSCCodesCleanup semicolonCleanup = new ConvertMSCCodesCleanup(new BibEntryPreferences(','), false);
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "03E72, Machine vision and scene understanding");

        List<FieldChange> changes = semicolonCleanup.cleanup(entry);
        // "68T45": "Machine vision and scene understanding"
        assertEquals("03E72,68T45", changes.getFirst().getNewValue());
    }

    @Test
    void cleanupReturnsEmptyListForEmptyKeywords() {
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "");

        List<FieldChange> changes = convertMscCleanup.cleanup(entry);

        assertTrue(changes.isEmpty());
    }

    @Test
    void cleanupReturnsEmptyListForNoChanges() {
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "Machine Learning, Artificial Intelligence");

        List<FieldChange> changes = convertMscCleanup.cleanup(entry);

        assertTrue(changes.isEmpty());
    }

    @Test
    void cleanupWorksWithDifferentSeparator() {
        // Test with semicolon separator
        ConvertMSCCodesCleanup semicolonCleanup = new ConvertMSCCodesCleanup(new BibEntryPreferences(';'), true);
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "03E75; Machine Learning");

        List<FieldChange> changes = semicolonCleanup.cleanup(entry);

        assertEquals("Applications of set theory;Machine Learning", changes.getFirst().getNewValue());
    }

    @Test
    void cleanupCanConvertDescriptionsBackToCodes() {
        // Test converting descriptions back to codes
        ConvertMSCCodesCleanup inverseCleanup = new ConvertMSCCodesCleanup(new BibEntryPreferences(','), false);
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "Applications of set theory, Machine Learning");

        List<FieldChange> changes = inverseCleanup.cleanup(entry);

        assertFalse(changes.isEmpty());
        assertEquals("03E75,Machine Learning", changes.getFirst().getNewValue());
    }
}
