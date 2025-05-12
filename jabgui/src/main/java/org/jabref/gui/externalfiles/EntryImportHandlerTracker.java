package org.jabref.gui.externalfiles;

import java.util.concurrent.atomic.AtomicInteger;

public class EntryImportHandlerTracker {
    private final AtomicInteger imported = new AtomicInteger(0);
    private final AtomicInteger skipped = new AtomicInteger(0);

    private final int totalEntries;
    private Runnable onFinish;

    public EntryImportHandlerTracker() {
        this(0);
    }

    public EntryImportHandlerTracker(int totalEntries) {
        this.totalEntries = totalEntries;
    }

    public void setOnFinish(Runnable onFinish) {
        this.onFinish = onFinish;
    }

    public void markImported() {
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
}
