package org.jabref.logic.pdf.search.indexing;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

/**
 * Wrapper around {@link LuceneIndexer} to execute all operations in the background.
 */
public class IndexingTaskManager extends BackgroundTask<Void> {

    private final ConcurrentLinkedDeque<Runnable> taskQueue = new ConcurrentLinkedDeque<>();
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
            taskQueue.pollFirst().run();
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
            updateMessage(Localization.lang("%0 of %1 entries added to the index", numOfIndexedFiles, numOfIndexedFiles + taskQueue.size()));
            updateProgress(numOfIndexedFiles, numOfIndexedFiles + taskQueue.size());
        });
    }

    private void enqueueTask(Runnable indexingTask, boolean skipToFront) {
        if (!isBlockingNewTasks) {
            if (skipToFront) {
                taskQueue.addFirst(indexingTask);
            } else {
                taskQueue.addLast(indexingTask);
            }
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

    public void createIndex(LuceneIndexer indexer) {
        enqueueTask(() -> indexer.createIndex(), true);
    }

    public void manageFulltextIndexAccordingToPrefs(LuceneIndexer indexer) {
        indexer.getFilePreferences().fulltextIndexLinkedFilesProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.booleanValue()) {
                for (BibEntry bibEntry : indexer.getDatabaseContext().getEntries()) {
                    enqueueTask(() -> indexer.updateLinkedFilesInIndex(bibEntry, List.of()), false);
                }
            } else {
                enqueueTask(() -> indexer.deleteLinkedFilesIndex(), true);
            }
        });
    }

    public void updateIndex(LuceneIndexer indexer) {
        Set<String> pathsToRemove = indexer.getListOfFilePaths();
        Set<Integer> hashesOfEntriesToRemove = indexer.getDatabaseContext().getEntries().stream().map(BibEntry::updateAndGetIndexHash).collect(Collectors.toSet());
        for (BibEntry entry : indexer.getDatabaseContext().getEntries()) {
            enqueueTask(() -> indexer.addBibFieldsToIndex(entry), true);
            enqueueTask(() -> indexer.addLinkedFilesToIndex(entry), false);
            hashesOfEntriesToRemove.removeIf(hash -> Integer.valueOf(entry.getLastIndexHash()).equals(hash));
            for (LinkedFile file : entry.getFiles()) {
                pathsToRemove.remove(file.getLink());
            }
        }
        for (String pathToRemove : pathsToRemove) {
            enqueueTask(() -> indexer.removeFromIndex(pathToRemove), true);
        }
        for (int hashToRemove : hashesOfEntriesToRemove) {
            enqueueTask(() -> indexer.removeFromIndex(hashToRemove), true);
        }
    }

    public void addToIndex(LuceneIndexer indexer, BibEntry entry) {
        enqueueTask(() -> indexer.addBibFieldsToIndex(entry), true);
        enqueueTask(() -> indexer.addLinkedFilesToIndex(entry), false);
    }

    public void removeFromIndex(LuceneIndexer indexer, BibEntry entry) {
        enqueueTask(() -> indexer.removeFromIndex(entry), false);
    }

    public void updateIndex(LuceneIndexer indexer, BibEntry entry, List<LinkedFile> removedFiles) {
        enqueueTask(() -> indexer.updateBibFieldsInIndex(entry), true);
        enqueueTask(() -> indexer.updateLinkedFilesInIndex(entry, removedFiles), false);
    }

    public void updateDatabaseName(String name) {
        DefaultTaskExecutor.runInJavaFXThread(() -> this.titleProperty().set(Localization.lang("Indexing for %0", name)));
    }
}
