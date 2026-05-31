package org.jabref.logic.ai.ingestion.tasks.generateembeddings;

import org.jabref.logic.ai.ingestion.logic.ingestion.LinkedFileIngestor;
import org.jabref.logic.ai.util.TrackedBackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// This task generates embeddings for a {@link LinkedFile}.
/// It will check if embeddings were already generated.
/// And it also will store the embeddings.
public class GenerateEmbeddingsTask extends TrackedBackgroundTask<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEmbeddingsTask.class);

    private final GenerateEmbeddingsTaskRequest request;

    private final LinkedFileIngestor linkedFileIngestor;

    public GenerateEmbeddingsTask(
            GenerateEmbeddingsTaskRequest request
    ) {
        this.request = request;
        this.linkedFileIngestor = new LinkedFileIngestor(
                request.filePreferences(),
                request.ingestedDocumentsRepository(),
                request.embeddingStore(),
                request.embeddingModel(),
                request.documentSplitter()
        );

        configure();
    }

    private void configure() {
        showToUser(true);
        titleProperty().set(Localization.lang("Generating embeddings for file '%0'", request.linkedFile().getLink()));
    }

    @Override
    public Void perform() {
        LOGGER.debug("Starting embeddings generation task");

        try {
            linkedFileIngestor.ingest(
                    request.bibDatabaseContext(),
                    request.linkedFile()
            );
        } catch (InterruptedException e) {
            LOGGER.debug("There is a embeddings generation task. It will be cancelled, because user quits JabRef");
        }

        LOGGER.debug("Finished embeddings generation task");
        progressCounter.stop();
        return null;
    }

    public LinkedFile getLinkedFile() {
        return request.linkedFile();
    }
}
