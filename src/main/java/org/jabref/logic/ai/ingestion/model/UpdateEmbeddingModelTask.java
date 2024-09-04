package org.jabref.logic.ai.ingestion.model;

import java.io.IOException;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.preferences.ai.AiPreferences;

import ai.djl.MalformedModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateEmbeddingModelTask extends BackgroundTask<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateEmbeddingModelTask.class);

    private static final String DJL_EMBEDDING_MODEL_URL_PREFIX = "djl://ai.djl.huggingface.pytorch/";

    private final AiPreferences aiPreferences;
    private final ObjectProperty<Optional<DeepJavaEmbeddingModel>> predictorProperty;

    private final ProgressCounter progressCounter = new ProgressCounter();

    public UpdateEmbeddingModelTask(AiPreferences aiPreferences, ObjectProperty<Optional<DeepJavaEmbeddingModel>> predictorProperty) {
        this.aiPreferences = aiPreferences;
        this.predictorProperty = predictorProperty;

        configure();
    }

    private void configure() {
        titleProperty().set(Localization.lang("Updating local embedding model..."));
        showToUser(true);

        progressCounter.listenToAllProperties(this::updateProgress);
    }

    @Override
    public Void call() {
        if (!aiPreferences.getEnableAi()) {
            predictorProperty.set(Optional.empty());
            return null;
        }

        LOGGER.info("Downloading embedding model...");

        String modelUrl = DJL_EMBEDDING_MODEL_URL_PREFIX + aiPreferences.getEmbeddingModel().getName();

        Criteria<String, float[]> criteria =
                Criteria.builder()
                        .setTypes(String.class, float[].class)
                        .optModelUrls(modelUrl)
                        .optEngine("PyTorch")
                        .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                        .optProgress(progressCounter)
                        .build();

        try {
            predictorProperty.set(Optional.of(new DeepJavaEmbeddingModel(criteria)));
        } catch (ModelNotFoundException e) {
            predictorProperty.set(Optional.empty());
            throw new RuntimeException(Localization.lang("Unable to find the embedding model by the URL %0", modelUrl), e);
        } catch (MalformedModelException e) {
            predictorProperty.set(Optional.empty());
            throw new RuntimeException(Localization.lang("The model by URL %0 is malformed", modelUrl), e);
        } catch (IOException e) {
            predictorProperty.set(Optional.empty());
            throw new RuntimeException(Localization.lang("An I/O error occurred while opening the embedding model by URL %0", modelUrl), e);
        }

        progressCounter.stop();

        return null;
    }

    private void updateProgress() {
        updateProgress(progressCounter.getWorkDone(), progressCounter.getWorkMax());
        updateMessage(progressCounter.getMessage());
    }
}
