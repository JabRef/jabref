package org.jabref.gui.externalfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.Nullable;

public class EntryImportHandlerTracker {
    private final AtomicInteger imported = new AtomicInteger(0);
    private final AtomicInteger skipped = new AtomicInteger(0);
    private final List<BibEntry> importedEntries;

    private final int totalEntries;
    private @Nullable Runnable onFinish;

    public EntryImportHandlerTracker() {
        this(0);
    }

    public EntryImportHandlerTracker(int totalEntries) {
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
        checkDone(totalProcessed);
    }

    public void markSkipped() {
        int totalProcessed = imported.get() + skipped.incrementAndGet();
        checkDone(totalProcessed);
    }

    private void checkDone(int totalProcessed) {
        if (totalProcessed >= totalEntries && onFinish != null) {
            onFinish.run();
        }
    }

    public int getImportedCount() {
        return imported.get();
    }

    public int getSkippedCount() {
        return skipped.get();
    }

    public List<BibEntry> getImportedEntries() {
        return importedEntries;
    }
}
