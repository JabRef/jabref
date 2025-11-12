package org.jabref.gui.externalfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
public class EntryImportHandlerTracker {
    private final AtomicInteger imported = new AtomicInteger(0);
    private final AtomicInteger skipped = new AtomicInteger(0);
    private final List<BibEntry> importedEntries;

    private final @NonNull StateManager stateManager;
    private final int totalEntries;
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

    public void setOnFinish(@Nullable Runnable onFinish) {
        this.onFinish = onFinish;
    }

    /// Marks the given entry as imported
    public void markImported(BibEntry entry) {
        int totalProcessed = imported.incrementAndGet() + skipped.get();
        importedEntries.add(entry);
        checkDone(totalProcessed);
    }

    public void markSkipped() {
        int totalProcessed = imported.get() + skipped.incrementAndGet();
        checkDone(totalProcessed);
    }

    /// Checks if all entries have been imported; if yes, execute the onFinish action
    private void checkDone(int totalProcessed) {
        if (totalProcessed < totalEntries) {
            return;
        }
        if (onFinish != null) {
            onFinish.run();
        }
        stateManager.setSelectedEntries(importedEntries);
    }

    public int getSkippedCount() {
        return skipped.get();
    }
}
