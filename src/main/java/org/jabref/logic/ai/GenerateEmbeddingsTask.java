package org.jabref.logic.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.ai.embeddings.FileToDocument;
import org.jabref.logic.ai.ingestion.IngestionService;
import org.jabref.logic.ai.ingestion.IngestionState;
import org.jabref.logic.ai.ingestion.IngestionStatus;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import dev.langchain4j.data.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateEmbeddingsTask extends BackgroundTask<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEmbeddingsTask.class);

    private final IngestionStatus ingestionStatus;
    private final LinkedFile linkedFile;
    private final FileEmbeddingsManager fileEmbeddingsManager;
    private final BibDatabaseContext bibDatabaseContext;
    private final FilePreferences filePreferences;

    private final ProgressCounter progressCounter = new ProgressCounter();

    public GenerateEmbeddingsTask(IngestionStatus ingestionStatus,
                                  FileEmbeddingsManager fileEmbeddingsManager,
                                  BibDatabaseContext bibDatabaseContext,
                                  FilePreferences filePreferences) {
        this.ingestionStatus = ingestionStatus;
        this.linkedFile = ingestionStatus.linkedFile();
        this.fileEmbeddingsManager = fileEmbeddingsManager;
        this.bibDatabaseContext = bibDatabaseContext;
        this.filePreferences = filePreferences;

        titleProperty().set(Localization.lang("Generating embeddings for file '%0'", linkedFile.getLink()));
        showToUser(true);

        progressCounter.listenToAllProperties(this::updateProgress);
    }

    @Override
    protected Void call() throws Exception {
        LOGGER.info("Starting embeddings generation task for file \"{}\"", linkedFile.getLink());

        try {
            ingestLinkedFile(linkedFile);
        } catch (InterruptedException e) {
            LOGGER.info("There is a embeddings generation task for file \"{}\". It will be cancelled, because user quits JabRef.", linkedFile.getLink());
        }

        if (ingestionStatus.state().get() != IngestionState.INGESTION_FAILED) {
            ingestionStatus.state().set(IngestionState.INGESTION_SUCCESS);
        }

        showToUser(false);

        LOGGER.info("Finished embeddings generation task for file \"{}\"", linkedFile.getLink());

        return null;
    }

    private void ingestLinkedFile(LinkedFile linkedFile) throws InterruptedException {
        // Rationale for RuntimeException here:
        // See org.jabref.logic.ai.summarization.GenerateSummaryTask.summarizeAll

        LOGGER.info("Generating embeddings for file \"{}\"", linkedFile.getLink());

        Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);

        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file \"{}\", while generating embeddings", linkedFile.getLink());
            LOGGER.info("Unable to generate embeddings for file \"{}\", because it was not found while generating embeddings", linkedFile.getLink());
            setIngestionStatusError(Localization.lang("Could not find path for a linked file '%0' while generating embeddings", linkedFile.getLink()));
            return;
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
                    LOGGER.info("No need to generate embeddings for file \"{}\", because it was already generated", linkedFile.getLink());
                    shouldIngest = false;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Couldn't retrieve attributes of a linked file \"{}\"", linkedFile.getLink(), e);
            LOGGER.warn("Possibly regenerating embeddings for linked file \"{}\"", linkedFile.getLink());
        }

        if (!shouldIngest) {
            return;
        }

        Optional<Document> document = FileToDocument.fromFile(path.get());
        if (document.isPresent()) {
            fileEmbeddingsManager.addDocument(linkedFile.getLink(), document.get(), modTime.orElse(0L), progressCounter.workDoneProperty(), progressCounter.workMaxProperty());
            LOGGER.info("Embeddings for file \"{}\" were generated successfully", linkedFile.getLink());
        } else {
            LOGGER.error("Unable to generate embeddings for file \"{}\", because JabRef was unable to extract text from the file", linkedFile.getLink());
            setIngestionStatusError(Localization.lang("Unable to generate embeddings for file '%0', because JabRef was unable to extract text from the file", linkedFile.getLink()));
        }
    }

    private void updateProgress() {
        updateProgress(progressCounter.getWorkDone(), progressCounter.getWorkMax());
        updateMessage(progressCounter.getMessage());
    }

    private void setIngestionStatusError(String message) {
        ingestionStatus.state().set(IngestionState.INGESTION_FAILED);
        ingestionStatus.message().set(message);
    }
}
