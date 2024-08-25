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

    private final String citationKey;
    private final List<LinkedFile> linkedFiles;
    private final FileEmbeddingsManager fileEmbeddingsManager;
    private final BibDatabaseContext bibDatabaseContext;
    private final FilePreferences filePreferences;

    private final ProgressCounter progressCounter = new ProgressCounter();

    public GenerateEmbeddingsTask(String citationKey,
                                  List<LinkedFile> linkedFiles,
                                  FileEmbeddingsManager fileEmbeddingsManager,
                                  BibDatabaseContext bibDatabaseContext,
                                  FilePreferences filePreferences) {
        this.citationKey = citationKey;
        this.linkedFiles = linkedFiles;
        this.fileEmbeddingsManager = fileEmbeddingsManager;
        this.bibDatabaseContext = bibDatabaseContext;
        this.filePreferences = filePreferences;

        titleProperty().set(Localization.lang("Generating embeddings for %0", citationKey));
        showToUser(true);

        progressCounter.listenToAllProperties(this::updateProgress);
    }

    @Override
    protected Void call() throws Exception {
        LOGGER.info("Starting embeddings generation task for entry {}", citationKey);

        try {
            // forEach() method would look better here, but we need to catch the {@link InterruptedException}.
            for (LinkedFile linkedFile : linkedFiles) {
                ingestLinkedFile(linkedFile);
            }
        } catch (InterruptedException e) {
            LOGGER.info("There is a embeddings generation task for {}. It will be cancelled, because user quits JabRef.", citationKey);
        }

        showToUser(false);

        LOGGER.info("Finished embeddings generation task for entry {}", citationKey);

        progressCounter.stop();

        return null;
    }

    private void ingestLinkedFile(LinkedFile linkedFile) throws InterruptedException {
        // Rationale for RuntimeException here:
        // See org.jabref.logic.ai.summarization.GenerateSummaryTask.summarizeAll

        LOGGER.info("Generating embeddings for file \"{}\" of entry {}", linkedFile.getLink(), citationKey);

        Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);

        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file \"{}\", while generating embeddings for entry {}", linkedFile.getLink(), citationKey);
            LOGGER.info("Unable to generate embeddings for file \"{}\", because it was not found while generating embeddings for entry {}", linkedFile.getLink(), citationKey);
            throw new RuntimeException(Localization.lang("Could not find path for a linked file %0 while generating embeddings for entry %1", linkedFile.getLink(), citationKey));
        }

        try {
            BasicFileAttributes attributes = Files.readAttributes(path.get(), BasicFileAttributes.class);

            long currentModificationTimeInSeconds = attributes.lastModifiedTime().to(TimeUnit.SECONDS);

            Optional<Long> ingestedModificationTimeInSeconds = fileEmbeddingsManager.getIngestedDocumentModificationTimeInSeconds(linkedFile.getLink());

            if (ingestedModificationTimeInSeconds.isPresent() && currentModificationTimeInSeconds <= ingestedModificationTimeInSeconds.get()) {
                LOGGER.info("No need to generate embeddings for entry {} for file \"{}\", because it was already generated", citationKey, linkedFile.getLink());
                return;
            }

            Optional<Document> document = FileToDocument.fromFile(path.get());
            if (document.isPresent()) {
                fileEmbeddingsManager.addDocument(linkedFile.getLink(), document.get(), currentModificationTimeInSeconds, progressCounter.workDoneProperty(), progressCounter.workMaxProperty());
                LOGGER.info("Embeddings for file \"{}\" were generated successfully, while processing entry {}", linkedFile.getLink(), citationKey);
            } else {
                LOGGER.error("Unable to generate embeddings for file \"{}\", because JabRef was unable to extract text from the file, while processing entry {}", linkedFile.getLink(), citationKey);
                throw new RuntimeException(Localization.lang("Unable to generate embeddings for file %0, because JabRef was unable to extract text from the file, while processing entry %1", linkedFile.getLink(), citationKey));
            }
        } catch (IOException e) {
            LOGGER.error("Couldn't retrieve attributes of a linked file \"{}\", while generating embeddings for entry {}", linkedFile.getLink(), citationKey, e);
            LOGGER.warn("Regenerating embeddings for linked file \"{}\", while processing entry {}", linkedFile.getLink(), citationKey);

            Optional<Document> document = FileToDocument.fromFile(path.get());
            if (document.isPresent()) {
                fileEmbeddingsManager.addDocument(linkedFile.getLink(), document.get(), 0, progressCounter.workDoneProperty(), progressCounter.workMaxProperty());
                LOGGER.info("Embeddings for file \"{}\" were generated successfully while processing entry {}, but the JabRef couldn't check if the file was changed", linkedFile.getLink(), citationKey);
            } else {
                LOGGER.info("Unable to generate embeddings for file \"{}\" while processing entry {}, because JabRef was unable to extract text from the file", linkedFile.getLink(), citationKey);
            }
        }
    }

    private void updateProgress() {
        updateProgress(progressCounter.getWorkDone(), progressCounter.getWorkMax());
        updateMessage(progressCounter.getMessage());
    }
}
