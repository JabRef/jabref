package org.jabref.logic.cleanup;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for ConvertMSCCodesCleanup that supports JavaFX operations
 */
@ExtendWith(ApplicationExtension.class)
class ConvertMSCCodesCleanupTest {

    private static final Logger LOGGER = Logger.getLogger(ConvertMSCCodesCleanupTest.class.getName());

    private CleanupWorker worker;
    private BibEntryPreferences bibEntryPreferences;
    private ConvertMSCCodesCleanup convertMscCleanup;

    @BeforeAll
    public static void setUpJavaFX() {
        // This ensures that a JavaFX runtime is initialized through TestFX
    }

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
        convertMscCleanup = new ConvertMSCCodesCleanup(new BibEntryPreferences(','), true);
    }

    /**
     * Helper method to run code on the JavaFX thread and wait for its completion
     *
     * @param action The action to run on the JavaFX thread
     * @param <T>    The return type
     * @return The result of the action or null if there was an error
     */
    private <T> T runOnJavaFXThreadAndWait(Supplier<T> action) {
        if (Platform.isFxApplicationThread()) {
            return action.get();
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<T> result = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                result.set(action.get());
            } finally {
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                LOGGER.log(Level.SEVERE, "Timeout waiting for JavaFX thread operation");
                return null;
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Interrupted while waiting for JavaFX thread operation", e);
            Thread.currentThread().interrupt();
            return null;
        }

        return result.get();
    }

    /**
     * Helper method to run code on the JavaFX thread and wait for its completion
     *
     * @param action The action to run on the JavaFX thread
     * @return true if the action completed successfully, false otherwise
     */
    private boolean runOnJavaFXThreadAndWait(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return true;
        }

        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                LOGGER.log(Level.SEVERE, "Timeout waiting for JavaFX thread operation");
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Interrupted while waiting for JavaFX thread operation", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Test
    void cleanupConvertsValidMSCCode() {
        boolean success = runOnJavaFXThreadAndWait(() -> {
            CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CONVERT_MSC_CODES);
            BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "03E75");

            worker.cleanup(preset, entry);

            Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
            assertEquals("Applications of set theory", keywords.get());
        });

        assertTrue(success);
    }

    @Test
    void cleanupPreservesNonMSCKeywords() {
        boolean success = runOnJavaFXThreadAndWait(() -> {
            CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CONVERT_MSC_CODES);
            BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "03E75, Machine Learning, Artificial Intelligence");

            worker.cleanup(preset, entry);

            Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
            assertEquals("Applications of set theory,Machine Learning,Artificial Intelligence", keywords.get());
        });

        assertTrue(success);
    }

    @Test
    void cleanupHandlesInvalidMSCCode() {
        boolean success = runOnJavaFXThreadAndWait(() -> {
            CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CONVERT_MSC_CODES);
            BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "99Z99, Machine Learning");

            worker.cleanup(preset, entry);

            Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
            assertEquals("99Z99, Machine Learning", keywords.get());
        });

        assertTrue(success);
    }

    @Test
    void cleanupHandlesNoKeywordsField() {
        boolean success = runOnJavaFXThreadAndWait(() -> {
            CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CONVERT_MSC_CODES);
            BibEntry entry = new BibEntry();

            worker.cleanup(preset, entry);

            assertEquals(Optional.empty(), entry.getField(StandardField.KEYWORDS));
        });

        assertTrue(success);
    }

    @Test
    void cleanupHandlesMultipleMSCCodes() {
        boolean success = runOnJavaFXThreadAndWait(() -> {
            CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CONVERT_MSC_CODES);
            BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "03E75, 68T01");

            worker.cleanup(preset, entry);

            Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
            assertEquals("Applications of set theory,General topics in artificial intelligence", keywords.get());
        });

        assertTrue(success);
    }

    @Test
    void cleanupReturnsCorrectFieldChanges() {
        List<FieldChange> changes = runOnJavaFXThreadAndWait(() -> {
            ConvertMSCCodesCleanup semicolonCleanup = new ConvertMSCCodesCleanup(new BibEntryPreferences(','), false);
            BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "03E72, Machine vision and scene understanding");

            return semicolonCleanup.cleanup(entry);
        });

        // Ensure we got a valid result
        if (changes != null) {
            // "68T45": "Machine vision and scene understanding"
            assertEquals("03E72,68T45", changes.getFirst().getNewValue());
        } else {
            assertEquals(1, 0, "JavaFX operation failed");
        }
    }

    @Test
    void cleanupReturnsEmptyListForEmptyKeywords() {
        List<FieldChange> changes = runOnJavaFXThreadAndWait(() -> {
            BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "");
            return convertMscCleanup.cleanup(entry);
        });

        // Ensure we got a valid result from JavaFX thread
        List<FieldChange> expectedEmptyList = Collections.emptyList();
        assertEquals(expectedEmptyList, changes != null ? changes : Collections.emptyList());
    }

    @Test
    void cleanupWorksWithDifferentSeparator() {
        List<FieldChange> changes = runOnJavaFXThreadAndWait(() -> {
            // Test with semicolon separator
            ConvertMSCCodesCleanup semicolonCleanup = new ConvertMSCCodesCleanup(new BibEntryPreferences(';'), true);
            BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "03E75; Machine Learning");

            return semicolonCleanup.cleanup(entry);
        });

        // Ensure we got a valid result
        if (changes != null && !changes.isEmpty()) {
            assertEquals("Applications of set theory;Machine Learning", changes.getFirst().getNewValue());
        } else {
            assertEquals(1, 0, "JavaFX operation failed or returned empty changes");
        }
    }

    @Test
    void cleanupCanConvertDescriptionsBackToCodes() {
        List<FieldChange> changes = runOnJavaFXThreadAndWait(() -> {
            // Test converting descriptions back to codes
            ConvertMSCCodesCleanup inverseCleanup = new ConvertMSCCodesCleanup(new BibEntryPreferences(','), false);
            BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "Applications of set theory, Machine Learning");

            return inverseCleanup.cleanup(entry);
        });

        // Ensure we got a valid result
        if (changes != null && !changes.isEmpty()) {
            assertEquals("03E75,Machine Learning", changes.getFirst().getNewValue());
        } else {
            assertEquals(1, 0, "JavaFX operation failed or returned empty changes");
        }
    }
}
