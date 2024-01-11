package org.jabref.logic.pdf.search;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

/**
 * Wrapper around {@link PdfIndexer} to execute all operations in the background.
 */
public class IndexingTaskManager extends BackgroundTask<Void> {

    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private TaskExecutor taskExecutor;
    private int numOfIndexedFiles = 0;

    private final Object lock = new Object();
    private boolean isRunning = false;
    private boolean isBlockingNewTasks = false;

    public IndexingTaskManager(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        showToUser(true);
        willBeRecoveredAutomatically(true);
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            this.updateProgress(1, 1);
            this.titleProperty().set(Localization.lang("Indexing pdf files"));
        });
    }

    @Override
    protected Void call() throws Exception {
        synchronized (lock) {
            isRunning = true;
        }
        updateProgress();
        while (!taskQueue.isEmpty() && !isCanceled()) {
            taskQueue.poll().run();
            numOfIndexedFiles++;
            updateProgress();
        }
        synchronized (lock) {
            isRunning = false;
        }
        return null;
    }

    private void updateProgress() {
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            updateMessage(Localization.lang("%0 of %1 linked files added to the index", numOfIndexedFiles, numOfIndexedFiles + taskQueue.size()));
            updateProgress(numOfIndexedFiles, numOfIndexedFiles + taskQueue.size());
        });
    }

    private void enqueueTask(Runnable indexingTask) {
        if (!isBlockingNewTasks) {
            taskQueue.add(indexingTask);
            // What if already running?
            synchronized (lock) {
                if (!isRunning) {
                    isRunning = true;
                    this.executeWith(taskExecutor);
                    showToUser(false);
                }
            }
        }
    }

    public AutoCloseable blockNewTasks() {
        synchronized (lock) {
            isBlockingNewTasks = true;
        }
        return () -> {
            synchronized (lock) {
                isBlockingNewTasks = false;
            }
        };
    }

    public void rebuildIndex(PdfIndexer indexer) {
        enqueueTask(indexer::rebuildIndex);
    }

    /**
     * Updates the index by performing a delta analysis of the files already existing in the index and the files in the library.
     */
    public void updateIndex(PdfIndexer indexer, BibDatabaseContext databaseContext) {
        Set<String> pathsToRemove = indexer.getListOfFilePaths();
        databaseContext.getEntries().stream()
                       .flatMap(entry -> entry.getFiles().stream())
                       .map(LinkedFile::getLink)
                       .forEach(pathsToRemove::remove);
        // The indexer checks the attached PDFs for modifications (based on the timestamp of the PDF) and reindexes the PDF if it is newer than the index. Therefore, we need to pass the whole library to the indexer for re-indexing.
        addToIndex(indexer, databaseContext.getEntries());
        enqueueTask(() -> indexer.removePathsFromIndex(pathsToRemove));
    }

    public void addToIndex(PdfIndexer indexer, List<BibEntry> entries) {
        AtomicInteger counter = new AtomicInteger();
        // To enable seeing progress in the UI, we group the entries in chunks of 50
        // Solution inspired by https://stackoverflow.com/a/27595803/873282
        entries.stream().collect(Collectors.groupingBy(x -> counter.getAndIncrement() / 50))
               .values()
               .forEach(list -> enqueueTask(() -> indexer.addToIndex(list)));
    }

    public void addToIndex(PdfIndexer indexer, BibEntry entry) {
        enqueueTask(() -> indexer.addToIndex(entry));
    }

    public void addToIndex(PdfIndexer indexer, BibEntry entry, List<LinkedFile> linkedFiles) {
        enqueueTask(() -> indexer.addToIndex(entry, linkedFiles));
    }

    public void removeFromIndex(PdfIndexer indexer, BibEntry entry) {
        enqueueTask(() -> indexer.removeFromIndex(entry));
    }

    public void removeFromIndex(PdfIndexer indexer, List<LinkedFile> linkedFiles) {
        enqueueTask(() -> indexer.removeFromIndex(linkedFiles));
    }

    public void updateDatabaseName(String name) {
        DefaultTaskExecutor.runInJavaFXThread(() -> this.titleProperty().set(Localization.lang("Indexing for %0", name)));
    }
}
