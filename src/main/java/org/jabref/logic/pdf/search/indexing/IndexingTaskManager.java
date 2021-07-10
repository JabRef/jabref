package org.jabref.logic.pdf.search.indexing;

import java.util.List;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

/**
 * Wrapper around {@link PdfIndexer} to execute all operations in the background.
 */
public class IndexingTaskManager extends BackgroundTask<Void> {

    BackgroundTask<Void> lastTaskInQueue;
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
        // @todo is this any good? Is there a better way?
        if (lastTaskInQueue == null) {
            task.executeWith(taskExecutor);
            this.progressProperty().bind(task.progressProperty());
        } else {
            lastTaskInQueue.onFinished(() -> {
                task.executeWith(taskExecutor);
                this.progressProperty().bind(task.progressProperty());
            });
        }
        lastTaskInQueue = task;
        lastTaskInQueue.onFinished(() -> {
            this.progressProperty().unbind();
            this.updateProgress(1, 1);
            lastTaskInQueue = null;
        });
    }

    public void createIndex(PdfIndexer indexer, ParserResult parserResult) {
        enqueueTask(new BackgroundTask<Void>() {
            @Override
            protected Void call() throws Exception {
                this.updateProgress(-1, 1);
                indexer.createIndex(parserResult);
                return null;
            }
        });
    }

    public void createIndex(PdfIndexer indexer, BibDatabase database, BibDatabaseContext context) {
        enqueueTask(new BackgroundTask<Void>() {
            @Override
            protected Void call() throws Exception {
                this.updateProgress(-1, 1);
                indexer.createIndex(database, context);
                return null;
            }
        });
    }

    public void addToIndex(PdfIndexer indexer, BibDatabaseContext databaseContext) {
        enqueueTask(new BackgroundTask<Void>() {
            @Override
            protected Void call() throws Exception {
                this.updateProgress(-1, 1);
                indexer.addToIndex(databaseContext);
                return null;
            }
        });
    }

    public void addToIndex(PdfIndexer indexer, BibEntry entry, BibDatabaseContext databaseContext) {
        enqueueTask(new BackgroundTask<Void>() {
            @Override
            protected Void call() throws Exception {
                this.updateProgress(-1, 1);
                indexer.addToIndex(entry, databaseContext);
                return null;
            }
        });
    }

    public void addToIndex(PdfIndexer indexer, BibEntry entry, List<LinkedFile> linkedFiles, BibDatabaseContext databaseContext) {
        enqueueTask(new BackgroundTask<Void>() {
            @Override
            protected Void call() throws Exception {
                this.updateProgress(-1, 1);
                indexer.addToIndex(entry, linkedFiles, databaseContext);
                return null;
            }
        });
    }

    public void addToIndex(PdfIndexer indexer, BibEntry entry, LinkedFile linkedFile, BibDatabaseContext databaseContext) {
        enqueueTask(new BackgroundTask<Void>() {
            @Override
            protected Void call() throws Exception {
                this.updateProgress(-1, 1);
                indexer.addToIndex(entry, linkedFile, databaseContext);
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

    public void removeFromIndex(PdfIndexer indexer, BibEntry entry, LinkedFile linkedFile) {
        enqueueTask(new BackgroundTask<Void>() {
            @Override
            protected Void call() throws Exception {
                this.updateProgress(-1, 1);
                indexer.removeFromIndex(entry, linkedFile);
                return null;
            }
        });
    }

    public void removeFromIndex(PdfIndexer indexer, BibDatabaseContext databaseContext) {
        enqueueTask(new BackgroundTask<Void>() {
            @Override
            protected Void call() throws Exception {
                int progressCounter = 0;
                this.updateProgress(progressCounter, databaseContext.getEntries().size());
                for (BibEntry entry : databaseContext.getEntries()) {
                    indexer.removeFromIndex(entry);
                    progressCounter++;
                    this.updateProgress(progressCounter, databaseContext.getEntries().size());
                }
                return null;
            }
        });
    }

    public void flushIndex(PdfIndexer indexer) {
        enqueueTask(new BackgroundTask<Void>() {
            @Override
            protected Void call() throws Exception {
                this.updateProgress(-1, 1);
                indexer.flushIndex();
                return null;
            }
        });
    }
}
