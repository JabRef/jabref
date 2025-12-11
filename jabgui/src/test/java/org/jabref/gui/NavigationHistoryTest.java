package org.jabref.gui;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationHistoryTest {

    @Test
    void addsEntriesAndNavigatesBackAndForward() {
        NavigationHistory history = new NavigationHistory();
        BibEntry first = new BibEntry(StandardEntryType.Article);
        BibEntry second = new BibEntry(StandardEntryType.Book);

        history.add(first);
        history.add(second);

        assertTrue(history.canGoBack());
        Optional<BibEntry> back = history.back();
        assertTrue(back.isPresent());
        assertEquals(first, back.get());

        assertTrue(history.canGoForward());
        Optional<BibEntry> forward = history.forward();
        assertTrue(forward.isPresent());
        assertEquals(second, forward.get());
    }

    @Test
    void suppressedAddsDoNotAffectHistory() {
        NavigationHistory history = new NavigationHistory();
        BibEntry initial = new BibEntry(StandardEntryType.Article);
        BibEntry suppressed = new BibEntry(StandardEntryType.Book);
        BibEntry later = new BibEntry(StandardEntryType.InProceedings);

        history.add(initial);

        try (NavigationHistory.Suppression ignored = history.suppressUpdates()) {
            history.add(suppressed);
        }

        // still only the initial entry is tracked
        assertFalse(history.canGoBack());

        history.add(later);

        assertTrue(history.canGoBack());
        Optional<BibEntry> back = history.back();
        assertTrue(back.isPresent());
        assertEquals(initial, back.get());
    }
}

