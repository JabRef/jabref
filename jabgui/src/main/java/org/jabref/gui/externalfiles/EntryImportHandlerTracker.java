package org.jabref.gui.externalfiles;

import java.util.ArrayList;
import java.util.List;

import org.jabref.gui.StateManager;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Tracks the import state
///
/// SIDE EFFECTS:
///
/// 1. Calls `onFinish` in case all entries have been imported (based on the given totalEntries count)
/// 2. Selects the imported entries after importing
///
/// Imports run concurrently: `ImportHandler` schedules one `BackgroundTask` chain per entry, so
/// `markImported`/`markSkipped` are invoked from different threads. The mutating methods are therefore
/// `synchronized` so that the counter update, list append, completion check, and `onFinish` execution
/// happen atomically. This guarantees `onFinish` runs exactly once and only after every imported entry
/// is visible in `importedEntries`.
public class EntryImportHandlerTracker {
    private final List<BibEntry> importedEntries;

    private final @NonNull StateManager stateManager;
    private final int totalEntries;
    private int imported = 0;
    private int skipped = 0;
    private boolean finished = false;
    private @Nullable Runnable onFinish;

    public EntryImportHandlerTracker(StateManager stateManager) {
        this(stateManager, 1);
    }

    public EntryImportHandlerTracker(StateManager stateManager, int totalEntries) {
        this.stateManager = stateManager;
        this.totalEntries = totalEntries;
        if (totalEntries > 0) {
            importedEntries = new ArrayList<>(totalEntries);
        } else {
            importedEntries = new ArrayList<>();
        }
    }

    public synchronized void setOnFinish(@Nullable Runnable onFinish) {
        this.onFinish = onFinish;
    }

    /// Marks the given entry as imported
    public synchronized void markImported(BibEntry entry) {
        imported++;
        importedEntries.add(entry);
        checkDone();
    }

    public synchronized void markSkipped() {
        skipped++;
        checkDone();
    }

    /// Checks if all entries have been processed; if yes, execute the onFinish action exactly once.
    /// Must be called while holding this object's monitor (i.e. from a `synchronized` method).
    private void checkDone() {
        if (finished || (imported + skipped) < totalEntries) {
            return;
        }
        finished = true;
        if (onFinish != null) {
            onFinish.run();
        }
        stateManager.setSelectedEntries(List.copyOf(importedEntries));
    }

    /// Returns an immutable snapshot of the actually imported `BibEntry` instances (the copies inserted
    /// into the database), not the originals passed to the import call.
    public synchronized List<BibEntry> getImportedEntries() {
        return List.copyOf(importedEntries);
    }

    public synchronized int getImportedCount() {
        return imported;
    }

    public synchronized int getSkippedCount() {
        return skipped;
    }
}
