package org.jabref.logic.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.impl.FileToDocument;
import org.jabref.logic.ai.impl.embeddings.EmbeddingGenerationBackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.search.PdfIndexerManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages a queue of embedding generation tasks for one {@link BibEntry} in a {@link org.jabref.model.database.BibDatabase}.
 * <p>
 * {@link org.jabref.model.database.BibDatabase} will listen for entries updates and add them to the queue. This class
 * will start a background task for generating the embeddings for each entry in the queue.
 * <p>
 * This class also provides an ability to prioritize the entry in the queue. But it seems not to work well.
 */
public class AiEmbeddingsTaskManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiEmbeddingsTaskManager.class);

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final AiService aiService;
    private final TaskExecutor taskExecutor;

    // We use an {@link ArrayList} as a queue to implement prioritization of the {@link LinkedFile}s as it provides more
    // methods to manipulate the collection.
    private final List<LinkedFile> linkedFileQueue = Collections.synchronizedList(new ArrayList<>());

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isBlockingNewTasks = new AtomicBoolean(false);

    private final BooleanProperty shutdownProperty = new SimpleBooleanProperty(false);

    public AiEmbeddingsTaskManager(BibDatabaseContext databaseContext, FilePreferences filePreferences, AiService aiService, TaskExecutor taskExecutor) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
        this.aiService = aiService;
        this.taskExecutor = taskExecutor;

        setupListeningToPreferencesChanges();
    }

    private void setupListeningToPreferencesChanges() {
        aiService.getPreferences().onEmbeddingsParametersChange(this::invalidate);
    }

    public void addToStore(Collection<BibEntry> bibEntries) {
        bibEntries.forEach(this::addToStore);
    }

    public void addToStore(BibEntry bibEntry) {
        addToStore(bibEntry.getFiles());
    }

    public void addToStore(List<LinkedFile> linkedFiles) {
        linkedFiles.forEach(this::addToStore);
    }

    public void addToStore(LinkedFile linkedFile) {
        if (!isBlockingNewTasks.get()) {
            linkedFileQueue.add(linkedFile);

            if (!isRunning.get()) {
                runBackgroundTask();
            }
        }
    }

    private void runBackgroundTask() {
        new EmbeddingGenerationBackgroundTask(isRunning, shutdownProperty, linkedFileQueue, databaseContext, filePreferences, aiService.getEmbeddingsManager())
                .executeWith(taskExecutor);
    }

    public AutoCloseable blockNewTasks() {
        isBlockingNewTasks.set(true);

        return () -> {
            isBlockingNewTasks.set(false);
        };
    }

    public void removeFromStore(Collection<BibEntry> bibEntries) {
        bibEntries.forEach(this::removeFromStore);
    }

    public void removeFromStore(BibEntry bibEntry) {
        removeFromStore(bibEntry.getFiles());
    }

    public void removeFromStore(List<LinkedFile> linkedFiles) {
        linkedFiles.forEach(this::removeFromStore);
    }

    public void removeFromStore(LinkedFile linkedFile) {
        aiService.getEmbeddingsManager().removeDocument(linkedFile.getLink());
    }

    public void removeFromStore(Set<String> linksToRemove) {
        linksToRemove.forEach(this::removeFromStore);
    }

    public void removeFromStore(String link) {
        aiService.getEmbeddingsManager().removeDocument(link);
    }

    public void updateEmbeddings(BibDatabaseContext bibDatabaseContext) {
        Set<String> linksToRemove = aiService.getEmbeddingsManager().getIngestedDocuments();
        bibDatabaseContext.getEntries().stream()
                       .flatMap(entry -> entry.getFiles().stream())
                       .map(LinkedFile::getLink)
                       .forEach(linksToRemove::remove);

        removeFromStore(linksToRemove);

        addToStore(bibDatabaseContext.getEntries());
    }

    public void invalidate() {
        linkedFileQueue.clear();
        databaseContext.getEntries().forEach(entry -> {
            removeFromStore(entry);
            addToStore(entry);
        });
    }

    public void shutdown() {
        linkedFileQueue.clear();
        shutdownProperty.set(true);
    }
}
