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
import org.jabref.logic.ai.impl.embeddings.LowLevelIngestor;
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
 *
 * FIXME: This is a "Manager" (similar to {@link PdfIndexerManager}) and should be renamed to "EmbeddingsGenerationManager".
 */
public class AiEmbeddingsGenerationTask extends BackgroundTask<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiEmbeddingsGenerationTask.class);

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final AiService aiService;
    private final TaskExecutor taskExecutor;

    // We use an {@link ArrayList} as a queue to implement prioritization of the {@link LinkedFile}s as it provides more
    // methods to manipulate the collection.
    private final List<LinkedFile> linkedFileQueue = Collections.synchronizedList(new ArrayList<>());
    private int numOfProcessedFiles = 0;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isBlockingNewTasks = new AtomicBoolean(false);

    private final BooleanProperty shutdownProperty = new SimpleBooleanProperty(false);

    public AiEmbeddingsGenerationTask(BibDatabaseContext databaseContext, FilePreferences filePreferences, AiService aiService, TaskExecutor taskExecutor) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
        this.aiService = aiService;
        this.taskExecutor = taskExecutor;

        configure();

        setupListeningToPreferencesChanges();
    }

    private void configure() {
        showToUser(true);
        willBeRecoveredAutomatically(true);
        titleProperty().set(Localization.lang("Embeddings generation"));

        this.onFailure(e -> {
            LOGGER.error("Failure during configure phase", e);
        });
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
                this.executeWith(taskExecutor);
            }
        }
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

    @Override
    protected Void call() throws Exception {
        isRunning.set(true);

        updateProgress();

        while (!linkedFileQueue.isEmpty() && !isCanceled()) {
            LinkedFile linkedFile = linkedFileQueue.removeLast();

            ingestLinkedFile(linkedFile);

            ++numOfProcessedFiles;
            updateProgress();
        }

        isRunning.set(false);

        return null;
    }

    private void ingestLinkedFile(LinkedFile linkedFile) {
        Optional<Path> path = linkedFile.findIn(databaseContext, filePreferences);

        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file: {}", linkedFile.getLink());
            return;
        }

        try {
            BasicFileAttributes attributes = Files.readAttributes(path.get(), BasicFileAttributes.class);

            long currentModificationTimeInSeconds = attributes.lastModifiedTime().to(TimeUnit.SECONDS);

            Optional<Long> ingestedModificationTimeInSeconds = aiService.getEmbeddingsManager().getIngestedDocumentModificationTimeInSeconds(linkedFile.getLink());

            if (ingestedModificationTimeInSeconds.isPresent() && currentModificationTimeInSeconds <= ingestedModificationTimeInSeconds.get()) {
                return;
            }

            FileToDocument.fromFile(path.get()).ifPresent(document ->
                    aiService.getEmbeddingsManager().addDocument(linkedFile.getLink(), document, currentModificationTimeInSeconds, shutdownProperty));
        } catch (IOException e) {
            LOGGER.error("Couldn't retrieve attributes of a linked file: {}", linkedFile.getLink(), e);
            LOGGER.warn("Regenerating embeddings for linked file: {}", linkedFile.getLink());

            FileToDocument.fromFile(path.get()).ifPresent(document ->
                    aiService.getEmbeddingsManager().addDocument(linkedFile.getLink(), document, 0, shutdownProperty));
        }
    }

    private void updateProgress() {
        updateMessage(Localization.lang("Generated embeddings %0 of %1 linked files", numOfProcessedFiles, numOfProcessedFiles + linkedFileQueue.size()));
        updateProgress(numOfProcessedFiles, numOfProcessedFiles + linkedFileQueue.size());
    }

    public void invalidate() {
        // TODO: Is this method right?
        // 1. How to stop if running.
        // 2. Is it okay to clear queue?
        // 3. Is it okay to 1) remove, 2) add to queue everything?

        linkedFileQueue.clear();
        databaseContext.getEntries().forEach(entry -> {
            removeFromStore(entry);
            addToStore(entry);
        });
    }

    public void updateDatabaseName(String name) {
        UiTaskExecutor.runInJavaFXThread(() -> this.titleProperty().set(Localization.lang("Generating embeddings for %0", name)));
    }

    public void shutdown() {
        LOGGER.trace("Shutting down embeddings generation task.");
        LOGGER.trace("Clearing linkedFileQueue...");
        linkedFileQueue.clear();
        LOGGER.trace("Cleared linkedFileQueue");
        shutdownProperty.set(true);
    }
}
