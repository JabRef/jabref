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
 *
 * <p>History updates can be suppressed during bulk operations (e.g., file imports, drag-and-drop)
 * using the {@link #suppressUpdates()} method, which employs a nesting-aware boolean flag to handle
 * multiple concurrent suppression contexts correctly.</p>
 */
public class NavigationHistory {
    private final List<BibEntry> previousEntries = new ArrayList<>();
    private final List<BibEntry> nextEntries = new ArrayList<>();
    private BibEntry currentEntry;
    private boolean suppressNavigation = false;

    /**
     * Sets a new entry as the current one, clearing the forward history.
     * The previously current entry is moved to the back stack.
     *
     * <p>This operation is skipped if navigation updates are currently suppressed.</p>
     *
     * @param entry The BibEntry to add to the history.
     */
    public void add(BibEntry entry) {
        if (suppressNavigation) {
            return;
        }

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

    /**
     * Suppresses navigation history updates while the returned guard is open.
     * Handles nesting correctly: if already suppressed, the flag is only cleared when the outermost guard closes.
     *
     * @return A {@link Suppression} guard that restores navigation tracking when closed.
     */
    public Suppression suppressUpdates() {
        return new Suppression(this);
    }

    /**
     * Convenience helper to suppress updates conditionally.
     *
     * @param active If true, returns an active suppression; otherwise returns a no-op suppression.
     * @return A {@link Suppression} guard.
     */
    public Suppression suppressUpdatesIf(boolean active) {
        return active ? suppressUpdates() : Suppression.noOp();
    }

    /**
     * AutoCloseable guard for suppressing navigation history updates.
     *
     * <p>Uses a nesting-aware approach: captures the suppression state when created,
     * and only restores it if this instance was the one that activated suppression.</p>
     */
    public static final class Suppression implements AutoCloseable {
        private static final Suppression NO_OP = new Suppression();

        private final NavigationHistory owner;
        private boolean closed;
        private final boolean active;
        private final boolean wasAlreadySuppressed;

        private Suppression() {
            this.owner = null;
            this.active = false;
            this.wasAlreadySuppressed = false;
        }

        private Suppression(NavigationHistory owner) {
            this.owner = owner;
            this.active = true;
            this.wasAlreadySuppressed = owner.suppressNavigation;
            owner.suppressNavigation = true;
        }

        @Override
        public void close() {
            if (closed || !active) {
                return;
            }
            closed = true;

            // Only turn off suppression if we were the ones who turned it on
            if (!wasAlreadySuppressed) {
                owner.suppressNavigation = false;
            }
        }

        public static Suppression noOp() {
            return NO_OP;
        }
    }
}
