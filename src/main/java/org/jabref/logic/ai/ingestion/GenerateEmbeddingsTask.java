package org.jabref.logic.ai.ingestion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task generates embeddings for a {@link LinkedFile}.
 * It will check if embeddings were already generated.
 * And it also will store the embeddings.
 */
public class GenerateEmbeddingsTask extends BackgroundTask<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEmbeddingsTask.class);

    private final LinkedFile linkedFile;
    private final FileEmbeddingsManager fileEmbeddingsManager;
    private final BibDatabaseContext bibDatabaseContext;
    private final FilePreferences filePreferences;
    private final ReadOnlyBooleanProperty shutdownSignal;

    private final ProgressCounter progressCounter = new ProgressCounter();

    public GenerateEmbeddingsTask(LinkedFile linkedFile,
                                  FileEmbeddingsManager fileEmbeddingsManager,
                                  BibDatabaseContext bibDatabaseContext,
                                  FilePreferences filePreferences,
                                  ReadOnlyBooleanProperty shutdownSignal
    ) {
        this.linkedFile = linkedFile;
        this.fileEmbeddingsManager = fileEmbeddingsManager;
        this.bibDatabaseContext = bibDatabaseContext;
        this.filePreferences = filePreferences;
        this.shutdownSignal = shutdownSignal;

        configure(linkedFile);
    }

    private void configure(LinkedFile linkedFile) {
        titleProperty().set(Localization.lang("Generating embeddings for file '%0'", linkedFile.getLink()));

        progressCounter.listenToAllProperties(this::updateProgress);
    }

    @Override
    public Void call() throws Exception {
        LOGGER.debug("Starting embeddings generation task for file \"{}\"", linkedFile.getLink());

        try {
            ingestLinkedFile(linkedFile);
        } catch (InterruptedException e) {
            LOGGER.debug("There is a embeddings generation task for file \"{}\". It will be cancelled, because user quits JabRef.", linkedFile.getLink());
        }

        LOGGER.debug("Finished embeddings generation task for file \"{}\"", linkedFile.getLink());
        progressCounter.stop();
        return null;
    }

    private void ingestLinkedFile(LinkedFile linkedFile) throws InterruptedException {
        // Rationale for RuntimeException here:
        // See org.jabref.logic.ai.summarization.GenerateSummaryTask.summarizeAll

        LOGGER.debug("Generating embeddings for file \"{}\"", linkedFile.getLink());

        Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);

        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file \"{}\", while generating embeddings", linkedFile.getLink());
            LOGGER.debug("Unable to generate embeddings for file \"{}\", because it was not found while generating embeddings", linkedFile.getLink());
            throw new RuntimeException(Localization.lang("Could not find path for a linked file '%0' while generating embeddings.", linkedFile.getLink()));
        }

        Optional<Long> modTime = Optional.empty();
        boolean shouldIngest = true;

        try {
            BasicFileAttributes attributes = Files.readAttributes(path.get(), BasicFileAttributes.class);

            long currentModificationTimeInSeconds = attributes.lastModifiedTime().to(TimeUnit.SECONDS);

            Optional<Long> ingestedModificationTimeInSeconds = fileEmbeddingsManager.getIngestedDocumentModificationTimeInSeconds(linkedFile.getLink());

            if (ingestedModificationTimeInSeconds.isEmpty()) {
                modTime = Optional.of(currentModificationTimeInSeconds);
            } else {
                if (currentModificationTimeInSeconds > ingestedModificationTimeInSeconds.get()) {
                    modTime = Optional.of(currentModificationTimeInSeconds);
                } else {
                    LOGGER.debug("No need to generate embeddings for file \"{}\", because it was already generated", linkedFile.getLink());
                    shouldIngest = false;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not retrieve attributes of a linked file \"{}\"", linkedFile.getLink(), e);
            LOGGER.warn("Possibly regenerating embeddings for linked file \"{}\"", linkedFile.getLink());
        }

        if (!shouldIngest) {
            return;
        }

        Optional<Document> document = new FileToDocument(shutdownSignal).fromFile(path.get());
        if (document.isPresent()) {
            fileEmbeddingsManager.addDocument(linkedFile.getLink(), document.get(), modTime.orElse(0L), progressCounter.workDoneProperty(), progressCounter.workMaxProperty());
            LOGGER.debug("Embeddings for file \"{}\" were generated successfully", linkedFile.getLink());
        } else {
            LOGGER.error("Unable to generate embeddings for file \"{}\", because JabRef was unable to extract text from the file", linkedFile.getLink());
            throw new RuntimeException(Localization.lang("Unable to generate embeddings for file '%0', because JabRef was unable to extract text from the file", linkedFile.getLink()));
        }
    }

    private void updateProgress() {
        updateProgress(progressCounter.getWorkDone(), progressCounter.getWorkMax());
        updateMessage(progressCounter.getMessage());
    }
}
