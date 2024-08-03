package org.jabref.logic.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javafx.beans.property.BooleanProperty;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.ai.embeddings.FileToDocument;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateEmbeddingsTask extends BackgroundTask<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEmbeddingsTask.class);

    private final List<LinkedFile> linkedFiles;
    private final FileEmbeddingsManager fileEmbeddingsManager;
    private final BibDatabaseContext bibDatabaseContext;
    private final FilePreferences filePreferences;
    private final BooleanProperty shutdownProperty;

    public GenerateEmbeddingsTask(String citationKey,
                                  List<LinkedFile> linkedFiles,
                                  FileEmbeddingsManager fileEmbeddingsManager,
                                  BibDatabaseContext bibDatabaseContext,
                                  FilePreferences filePreferences,
                                  BooleanProperty shutdownProperty) {
        this.linkedFiles = linkedFiles;
        this.fileEmbeddingsManager = fileEmbeddingsManager;
        this.bibDatabaseContext = bibDatabaseContext;
        this.filePreferences = filePreferences;
        this.shutdownProperty = shutdownProperty;

        showToUser(true);
        titleProperty().set(Localization.lang("Generating embeddings for for %0", citationKey));
    }

    @Override
    protected Void call() throws Exception {
        linkedFiles.forEach(this::ingestLinkedFile);
        showToUser(false);
        return null;
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

            Optional<Long> ingestedModificationTimeInSeconds = fileEmbeddingsManager.getIngestedDocumentModificationTimeInSeconds(linkedFile.getLink());

            if (ingestedModificationTimeInSeconds.isPresent() && currentModificationTimeInSeconds <= ingestedModificationTimeInSeconds.get()) {
                return;
            }

            FileToDocument.fromFile(path.get()).ifPresent(document ->
                    fileEmbeddingsManager.addDocument(linkedFile.getLink(), document, currentModificationTimeInSeconds, shutdownProperty));
        } catch (IOException e) {
            LOGGER.error("Couldn't retrieve attributes of a linked file: {}", linkedFile.getLink(), e);
            LOGGER.warn("Regenerating embeddings for linked file: {}", linkedFile.getLink());

            FileToDocument.fromFile(path.get()).ifPresent(document ->
                    fileEmbeddingsManager.addDocument(linkedFile.getLink(), document, 0, shutdownProperty));
        }
    }
}
