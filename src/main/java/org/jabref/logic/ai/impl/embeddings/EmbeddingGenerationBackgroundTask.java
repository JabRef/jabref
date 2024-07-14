package org.jabref.logic.ai.impl.embeddings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.property.BooleanProperty;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.ai.AiEmbeddingsManager;
import org.jabref.logic.ai.impl.FileToDocument;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddingGenerationBackgroundTask extends BackgroundTask<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingGenerationBackgroundTask.class);

    private final AtomicBoolean isRunning;
    private final BooleanProperty shutdownProperty;
    private final List<LinkedFile> linkedFileQueue;

    private final BibDatabaseContext bibDatabaseContext;
    private final FilePreferences filePreferences;
    private final AiEmbeddingsManager aiEmbeddingsManager;

    private int numOfIngestedFiles = 0;

    public EmbeddingGenerationBackgroundTask(AtomicBoolean isRunning,
                                             BooleanProperty shutdownProperty,
                                             List<LinkedFile> linkedFileQueue,
                                             BibDatabaseContext bibDatabaseContext,
                                             FilePreferences filePreferences,
                                             AiEmbeddingsManager aiEmbeddingsManager) {
        this.isRunning = isRunning;
        this.shutdownProperty = shutdownProperty;
        this.linkedFileQueue = linkedFileQueue;
        this.bibDatabaseContext = bibDatabaseContext;
        this.filePreferences = filePreferences;
        this.aiEmbeddingsManager = aiEmbeddingsManager;

        configure();
    }

    private void configure() {
        willBeRecoveredAutomatically(true);

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            titleProperty().set(Localization.lang("Embeddings generation"));
        } else {
            titleProperty().set(Localization.lang("Embeddings generation for " + bibDatabaseContext.getDatabasePath().get()));
        }

        this.onFailure(e -> {
            LOGGER.error("An error occurred in embeddings generation background task", e);
        });
    }

    @Override
    protected Void call() throws Exception {
        isRunning.set(true);
        showToUser(true);

        updateProgress();

        while (!linkedFileQueue.isEmpty() && !isCanceled()) {
            try {
                LinkedFile linkedFile = linkedFileQueue.removeLast();

                ingestLinkedFile(linkedFile);
                numOfIngestedFiles++;

                updateProgress();
            } catch (IndexOutOfBoundsException e) {
                LOGGER.debug("linkedFileQueue was manipulated. Skipping iteration");
            }
        }

        isRunning.set(false);
        showToUser(false);

        return null;
    }

    private void updateProgress() {
        updateProgress(numOfIngestedFiles, linkedFileQueue.size());
    }

    private void ingestLinkedFile(LinkedFile linkedFile) {
        Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);

        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file: {}", linkedFile.getLink());
            return;
        }

        try {
            BasicFileAttributes attributes = Files.readAttributes(path.get(), BasicFileAttributes.class);

            long currentModificationTimeInSeconds = attributes.lastModifiedTime().to(TimeUnit.SECONDS);

            Optional<Long> ingestedModificationTimeInSeconds = aiEmbeddingsManager.getIngestedDocumentModificationTimeInSeconds(linkedFile.getLink());

            if (ingestedModificationTimeInSeconds.isPresent() && currentModificationTimeInSeconds <= ingestedModificationTimeInSeconds.get()) {
                return;
            }

            FileToDocument.fromFile(path.get()).ifPresent(document ->
                    aiEmbeddingsManager.addDocument(linkedFile.getLink(), document, currentModificationTimeInSeconds, shutdownProperty));
        } catch (
                IOException e) {
            LOGGER.error("Couldn't retrieve attributes of a linked file: {}", linkedFile.getLink(), e);
            LOGGER.warn("Regenerating embeddings for linked file: {}", linkedFile.getLink());

            FileToDocument.fromFile(path.get()).ifPresent(document ->
                    aiEmbeddingsManager.addDocument(linkedFile.getLink(), document, 0, shutdownProperty));
        }
    }
}
