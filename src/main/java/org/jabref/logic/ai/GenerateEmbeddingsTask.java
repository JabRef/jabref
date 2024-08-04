package org.jabref.logic.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.ai.embeddings.FileToDocument;
import org.jabref.logic.l10n.Localization;
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

    private final IntegerProperty workDone = new SimpleIntegerProperty(0);
    private final IntegerProperty workMax = new SimpleIntegerProperty(0);

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

        titleProperty().set(Localization.lang("Generating embeddings for for %0", citationKey));
        showToUser(true);

        workDone.addListener(obs -> updateProgress());
        workMax.addListener(obs -> updateProgress());
    }

    @Override
    protected Void call() throws Exception {
        try {
            // forEach() method would look better here, but we need to catch the {@link InterruptedException}.
            for (LinkedFile linkedFile : linkedFiles) {
                ingestLinkedFile(linkedFile);
            }
        } catch (InterruptedException e) {
            LOGGER.info("There is a embeddings generation task for {}. It will be cancelled, because user quits JabRef.", citationKey);
        }

        showToUser(false);

        return null;
    }

    private void ingestLinkedFile(LinkedFile linkedFile) throws InterruptedException {
        Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);

        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file: {}", linkedFile.getLink());
            return;
        }

        try {
            BasicFileAttributes attributes = Files.readAttributes(path.get(), BasicFileAttributes.class);

            long currentModificationTimeInSeconds = attributes.lastModifiedTime().to(TimeUnit.SECONDS);

            Optional<Long> ingestedModificationTimeInSeconds = fileEmbeddingsManager.getIngestedDocumentModificationTimeInSeconds(linkedFile.getLink());

            if (ingestedModificationTimeInSeconds.isPresent() && currentModificationTimeInSeconds <= ingestedModificationTimeInSeconds.get()) {
                return;
            }

            Optional<Document> document = FileToDocument.fromFile(path.get());
            if (document.isPresent()) {
                fileEmbeddingsManager.addDocument(linkedFile.getLink(), document.get(), currentModificationTimeInSeconds, workDone, workMax);
            }
        } catch (IOException e) {
            LOGGER.error("Couldn't retrieve attributes of a linked file: {}", linkedFile.getLink(), e);
            LOGGER.warn("Regenerating embeddings for linked file: {}", linkedFile.getLink());

            Optional<Document> document = FileToDocument.fromFile(path.get());
            if (document.isPresent()) {
                fileEmbeddingsManager.addDocument(linkedFile.getLink(), document.get(), 0, workDone, workMax);
            }
        }
    }

    private void updateProgress() {
        updateProgress(workDone.get(), workMax.get());
        updateMessage(Localization.lang("%0% work done for embeddings generation", Math.round((float) workDone.get() / (float) workMax.get() * 100)));
    }
}
