package org.jabref.logic.pdf.search.indexing;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

/**
 * Wrapper around {@link PdfIndexer} to execute all operations in the background.
 */
public class IndexingTaskManager extends BackgroundTask<Void> {

    Queue<BackgroundTask<Void>> taskQueue = new LinkedList<>();
    TaskExecutor taskExecutor;

    public IndexingTaskManager(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        showToUser(true);
        // the task itself is a nop, but it's progress property will be updated by the child-tasks it creates that actually interact with the index
        this.updateProgress(1, 1);
        this.titleProperty().set(Localization.lang("Indexing pdf files"));
        this.executeWith(taskExecutor);
    }

    @Override
    protected Void call() throws Exception {
        // update index to make sure it is up to date
        this.updateProgress(1, 1);
        return null;
    }

    private void enqueueTask(BackgroundTask<Void> task) {
        task.onFinished(() -> {
            this.progressProperty().unbind();
            this.messageProperty().unbind();
            this.updateProgress(1, 1);
            taskQueue.poll(); // This is the task that just finished
            if (!taskQueue.isEmpty()) {
                BackgroundTask<Void> nextTask = taskQueue.poll();
                nextTask.executeWith(taskExecutor);
                this.progressProperty().bind(nextTask.progressProperty());
                this.messageProperty().bind(nextTask.messageProperty());
            }
        });
        taskQueue.add(task);
        if (taskQueue.size() == 1) {
            task.executeWith(taskExecutor);
            this.progressProperty().bind(task.progressProperty());
            this.messageProperty().bind(task.messageProperty());
        }
    }

    public void createIndex(PdfIndexer indexer) {
        enqueueTask(new BackgroundTask<Void>() {
            @Override
            protected Void call() throws Exception {
                this.updateProgress(-1, 1);
                indexer.createIndex();
                return null;
            }
        });
    }

    public void addToIndex(PdfIndexer indexer, BibDatabaseContext databaseContext) {
        enqueueTask(new BackgroundTask<Void>() {
            @Override
            protected Void call() throws Exception {
                this.updateProgress(-1, 1);
                final int numFiles = databaseContext.getEntries().stream().map(BibEntry::getFiles).map(List::size).mapToInt(Integer::intValue).sum();

                int progressCounter = 0;
                this.updateProgress(0, numFiles);
                this.updateMessage(Localization.lang("%0 of %1 files added to the index", 0, numFiles));

                for (BibEntry entry : databaseContext.getEntries()) {
                    for (LinkedFile file : entry.getFiles()) {
                        indexer.addToIndex(entry, file, databaseContext);
                        progressCounter++;
                        final int lambdaProgressCounter = progressCounter;
                        this.updateProgress(lambdaProgressCounter, numFiles);
                        this.updateMessage(Localization.lang("%0 of %1 files added to the index", lambdaProgressCounter, numFiles));
                    }
                }
                return null;
            }
        });
    }

    public void addToIndex(PdfIndexer indexer, BibEntry entry, BibDatabaseContext databaseContext) {
        enqueueTask(new BackgroundTask<Void>() {
            @Override
            protected Void call() throws Exception {
                this.updateProgress(-1, 1);
                final int numFiles = entry.getFiles().size();

                int progressCounter = 0;
                this.updateProgress(0, numFiles);
                this.updateMessage(Localization.lang("%0 of %1 files added to the index", 0, numFiles));

                for (LinkedFile file : entry.getFiles()) {
                    indexer.addToIndex(entry, file, databaseContext);
                    progressCounter++;
                    final int lambdaProgressCounter = progressCounter;
                    this.updateProgress(lambdaProgressCounter, numFiles);
                    this.updateMessage(Localization.lang("%0 of %1 files added to the index", lambdaProgressCounter, numFiles));
                }
                return null;
            }
        });
    }

    public void addToIndex(PdfIndexer indexer, BibEntry entry, List<LinkedFile> linkedFiles, BibDatabaseContext databaseContext) {
        enqueueTask(new BackgroundTask<Void>() {
            @Override
            protected Void call() throws Exception {
                this.updateProgress(0, 1);
                this.updateMessage(Localization.lang("%0 of %1 files added to the index", 0, 1));
                indexer.addToIndex(entry, linkedFiles, databaseContext);
                this.updateProgress(1, 1);
                this.updateMessage(Localization.lang("%0 of %1 files added to the index", 1, 1));
                return null;
            }
        });
    }

    public void removeFromIndex(PdfIndexer indexer, BibEntry entry, List<LinkedFile> linkedFiles) {
        enqueueTask(new BackgroundTask<Void>() {
            @Override
            protected Void call() throws Exception {
                this.updateProgress(-1, 1);
                indexer.removeFromIndex(entry, linkedFiles);
                return null;
            }
        });
    }

    public void removeFromIndex(PdfIndexer indexer, BibEntry entry) {
       enqueueTask(new BackgroundTask<Void>() {
           @Override
           protected Void call() throws Exception {
               this.updateProgress(-1, 1);
               indexer.removeFromIndex(entry);
               return null;
           }
       });
    }

    public void updateDatabaseName(String name) {
        this.titleProperty().set(Localization.lang("Indexing for %0", name));
    }
}
