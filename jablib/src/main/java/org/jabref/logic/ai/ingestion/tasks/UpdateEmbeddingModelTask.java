package org.jabref.logic.ai.ingestion.tasks;

import java.io.IOException;

import org.jabref.logic.ai.embedding.DeepJavaEmbeddingModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.ProgressCounter;

import ai.djl.MalformedModelException;
import ai.djl.repository.zoo.ModelNotFoundException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Downloads (or verifies) the local embedding model and returns the ready-to-use
/// {@link DeepJavaEmbeddingModel}. The caller is responsible for updating any
/// property / field once the task succeeds (via `onSuccess`).
public class UpdateEmbeddingModelTask extends BackgroundTask<DeepJavaEmbeddingModel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateEmbeddingModelTask.class);

    private final String embeddingModelName;

    private final ProgressCounter progressCounter = new ProgressCounter();

    public UpdateEmbeddingModelTask(String embeddingModelName) {
        this.embeddingModelName = embeddingModelName;

        configure();
    }

    private void configure() {
        titleProperty().set(Localization.lang("Downloading local embedding model..."));
        showToUser(true);

        progressCounter.listenToAllProperties(this::updateProgress);
    }

    @Override
    public @NonNull DeepJavaEmbeddingModel call() {
        LOGGER.info("Downloading embedding model...");

        boolean originallyDownloaded = DeepJavaEmbeddingModel.isDownloaded(embeddingModelName);

        try {
            DeepJavaEmbeddingModel model = new DeepJavaEmbeddingModel(
                    embeddingModelName,
                    progressCounter
            );

            if (!originallyDownloaded) {
                LOGGER.info("Embedding model was successfully downloaded");
            }

            return model;
        } catch (ModelNotFoundException e) {
            throw new RuntimeException(Localization.lang("Unable to find the embedding model \"%0\"", embeddingModelName), e);
        } catch (MalformedModelException e) {
            throw new RuntimeException(Localization.lang("The model \"%0\" is malformed", embeddingModelName), e);
        } catch (IOException e) {
            throw new RuntimeException(Localization.lang("An I/O error occurred while opening the embedding model \"%0\"", embeddingModelName), e);
        } finally {
            progressCounter.stop();
        }
    }

    private void updateProgress() {
        updateProgress(progressCounter.getWorkDone(), progressCounter.getWorkMax());
        updateMessage(progressCounter.getMessage());
    }
}
