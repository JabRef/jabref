package org.jabref.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

/**
 * Manages the navigation history of viewed entries using two stacks.
 * This class encapsulates the logic of moving back and forward by maintaining a "back" stack for past entries
 * and a "forward" stack for future entries.
 */
public class NavigationHistory {
    private final List<BibEntry> previousEntries = new ArrayList<>();
    private final List<BibEntry> nextEntries = new ArrayList<>();
    private BibEntry currentEntry;

    /**
     * Sets a new entry as the current one, clearing the forward history.
     * The previously current entry is moved to the back stack.
     *
     * @param entry The BibEntry to add to the history.
     */
    public void add(BibEntry entry) {
        if (Objects.equals(currentEntry, entry)) {
            return;
        }

        // a new selection invalidates the forward history
        nextEntries.clear();

        if (currentEntry != null) {
            previousEntries.add(currentEntry);
        }
        currentEntry = entry;
    }

    /**
     * Moves to the previous entry in the history.
     * The current entry is pushed to the forward stack, and the last entry from the back stack becomes current.
     *
     * @return An Optional containing the previous BibEntry, or an empty Optional if there's no history to go back to.
     */
    public Optional<BibEntry> back() {
        if (canGoBack()) {
            nextEntries.add(currentEntry);
            currentEntry = previousEntries.removeLast();
            return Optional.of(currentEntry);
        }
        return Optional.empty();
    }

    /**
     * Moves to the next entry in the history.
     * The current entry is pushed to the back stack, and the last entry from the forward stack becomes current.
     *
     * @return An Optional containing the next BibEntry, or an empty Optional if there is no "forward" history.
     */
    public Optional<BibEntry> forward() {
        if (canGoForward()) {
            previousEntries.add(currentEntry);
            currentEntry = nextEntries.removeLast();
            return Optional.of(currentEntry);
        }
        return Optional.empty();
    }

    public boolean canGoBack() {
        return !previousEntries.isEmpty();
    }

    public boolean canGoForward() {
        return !nextEntries.isEmpty();
    }
}
