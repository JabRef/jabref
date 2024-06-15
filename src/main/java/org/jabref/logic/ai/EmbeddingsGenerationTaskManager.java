package org.jabref.logic.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddingsGenerationTaskManager extends BackgroundTask<Void> {
    private final Logger LOGGER = LoggerFactory.getLogger(EmbeddingsGenerationTaskManager.class);

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final AiService aiService;
    private final TaskExecutor taskExecutor;

    private final Queue<LinkedFile> linkedFileQueue = new ConcurrentLinkedQueue<>();
    private final Object lock = new Object();
    private boolean isRunning = false;

    private int numOfProcessedFiles = 0;

    public EmbeddingsGenerationTaskManager(BibDatabaseContext databaseContext, FilePreferences filePreferences, AiService aiService, TaskExecutor taskExecutor) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
        this.aiService = aiService;
        this.taskExecutor = taskExecutor;

        showToUser(true);
        willBeRecoveredAutomatically(true);
        updateProgress(1, 1);
        titleProperty().set(Localization.lang("Embeddings generation"));

        this.onFailure(e -> {
            throw new RuntimeException(e);
        });
    }

    public void addToProcess(Collection<BibEntry> bibEntries) {
        bibEntries.forEach(this::addToProcess);
    }

    public void removeFromProcess(Collection<BibEntry> bibEntries) {
        bibEntries.forEach(this::removeFromProcess);
    }

    public void addToProcess(BibEntry bibEntry) {
        addToProcess(bibEntry.getFiles());
    }

    public void removeFromProcess(BibEntry bibEntry) {
        removeFromProcess(bibEntry.getFiles());
    }

    public void addToProcess(List<LinkedFile> linkedFiles) {
        linkedFiles.forEach(this::addToProcess);
    }

    public void removeFromProcess(List<LinkedFile> linkedFiles) {
        linkedFiles.forEach(this::removeFromProcess);
    }

    public void addToProcess(LinkedFile linkedFile) {
        linkedFileQueue.add(linkedFile);

        synchronized (lock) {
            if (!isRunning) {
                this.executeWith(taskExecutor);
                showToUser(false);
            }
        }
    }

    public void removeFromProcess(LinkedFile linkedFile) {
        aiService.removeIngestedFile(linkedFile.getLink());
    }

    public void removeFromProcess(Set<String> linksToRemove) {
        linksToRemove.forEach(this::removeFromProcess);
    }

    public void removeFromProcess(String link) {
        aiService.removeIngestedFile(link);
    }

    public void updateEmbeddings(BibDatabaseContext bibDatabaseContext) {
        Set<String> linksToRemove = aiService.getListOfIngestedFilesLinks();
        bibDatabaseContext.getEntries().stream()
                       .flatMap(entry -> entry.getFiles().stream())
                       .map(LinkedFile::getLink)
                       .forEach(linksToRemove::remove);

        removeFromProcess(linksToRemove);

        addToProcess(bibDatabaseContext.getEntries());
    }

    @Override
    protected Void call() throws Exception {
        synchronized (lock) {
            isRunning = true;
        }

        updateProgress();

        while (!linkedFileQueue.isEmpty() && !isCanceled()) {
            LinkedFile linkedFile = linkedFileQueue.poll();
            assert linkedFile != null;

            ingestLinkedFile(linkedFile);

            ++numOfProcessedFiles;
            updateProgress();
        }

        synchronized (lock) {
            isRunning = false;
        }

        return null;
    }

    private void ingestLinkedFile(LinkedFile linkedFile) {
        if (aiService.haveIngestedFile(linkedFile.getLink())) {
            return;
        }

        Optional<Path> path = linkedFile.findIn(databaseContext, filePreferences);
        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file: {}", linkedFile.getLink());
            return;
        }

        try {
            BasicFileAttributes attributes = Files.readAttributes(path.get(), BasicFileAttributes.class);

            long currentModificationTimeInSeconds = attributes.lastModifiedTime().to(TimeUnit.SECONDS);
            long ingestedModificationTimeInSeconds = aiService.getIngestedFileModificationTime(linkedFile.getLink());

            if (currentModificationTimeInSeconds <= ingestedModificationTimeInSeconds) {
                return;
            }
        } catch (IOException e) {
            LOGGER.error("Couldn't retrieve attributes of a linked file: {}", linkedFile.getLink(), e);
            LOGGER.warn("Regenerating embeddings for linked file: {}", linkedFile.getLink());
        }

        AiIngestor aiIngestor = new AiIngestor(aiService);
        aiIngestor.ingestLinkedFile(linkedFile, databaseContext, filePreferences);
    }

    private void updateProgress() {
        updateMessage(Localization.lang("Generated embeddings %0 of %1 linked files", numOfProcessedFiles, numOfProcessedFiles + linkedFileQueue.size()));
        updateProgress(numOfProcessedFiles, numOfProcessedFiles + linkedFileQueue.size());
    }
}
