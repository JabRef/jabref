package org.jabref.logic.cleanup;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ConvertMSCCodesCleanupTest {

    private ConvertMSCCodesCleanup worker;

    @BeforeAll
    static void initializeJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        latch.await();  // Wait for JavaFX thread to initialize
    }

    @BeforeEach
    void setUp() {
        BibEntryPreferences preferences = mock(BibEntryPreferences.class);
        // Simulate default separator
        Mockito.when(preferences.getKeywordSeparator()).thenReturn(',');
        worker = new ConvertMSCCodesCleanup(preferences, true);
    }

    private void runAndWait(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    action.run();
                } finally {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    private void runCleanupAndWait(BibEntry entry) {
        worker.cleanup(entry);
        runAndWait(() -> { });  // Wait for JavaFX to finish
    }

    @Test
    void cleanupConvertsValidMSCCode() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "03E72");

        runCleanupAndWait(entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertEquals("Theory of fuzzy sets - etc.", keywords.get());
    }

    @Test
    void cleanupPreservesNonMSCKeywords() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "03E72, Machine Learning, Artificial Intelligence");

        runCleanupAndWait(entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertEquals("Theory of fuzzy sets - etc.,Machine Learning,Artificial Intelligence", keywords.get());
    }

    @Test
    void cleanupHandlesInvalidMSCCode() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "99Z99, Machine Learning");

        runCleanupAndWait(entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertEquals("99Z99, Machine Learning", keywords.get());
    }

    @Test
    void cleanupHandlesNoKeywordsField() {
        BibEntry entry = new BibEntry();

        runCleanupAndWait(entry);

        assertEquals(Optional.empty(), entry.getField(StandardField.KEYWORDS));
    }

    @Test
    void cleanupHandlesMultipleMSCCodes() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "03E72, 68T01");

        runCleanupAndWait(entry);

        Optional<String> keywords = entry.getField(StandardField.KEYWORDS);
        assertEquals("Theory of fuzzy sets - etc.,General topics in artificial intelligence", keywords.get());
    }
}
