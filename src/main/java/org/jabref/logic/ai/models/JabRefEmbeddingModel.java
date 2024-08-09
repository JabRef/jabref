package org.jabref.logic.ai.models;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.ai.AiPreferences;

import ai.djl.MalformedModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.training.util.ProgressBar;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around langchain4j {@link dev.langchain4j.model.embedding.EmbeddingModel}.
 * <p>
 * This class listens to preferences changes.
 */
public class JabRefEmbeddingModel implements dev.langchain4j.model.embedding.EmbeddingModel, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefEmbeddingModel.class);

    private static final String DJL_EMBEDDING_MODEL_URL_PREFIX = "djl://ai.djl.huggingface.pytorch/";

    private final AiPreferences aiPreferences;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    private final ExecutorService executorService = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("ai-embedding-pool-%d").build()
    );

    private final ObjectProperty<Optional<DeepJavaEmbeddingModel>> predictorProperty = new SimpleObjectProperty<>(Optional.empty());

    // Used to update the tab content after the data is available
    private final EventBus eventBus = new EventBus();

    public static class EmbeddingModelBuiltEvent { }

    public static class EmbeddingModelBuildingErrorEvent { }

    // Empty if there is no error.
    private String errorWhileBuildingModel = "";

    public JabRefEmbeddingModel(AiPreferences aiPreferences, DialogService dialogService, TaskExecutor taskExecutor) {
        this.aiPreferences = aiPreferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        startRebuildingTask();

        setupListeningToPreferencesChanges();
    }

    public void registerListener(Object object) {
        eventBus.register(object);
    }

    public void startRebuildingTask() {
        if (!aiPreferences.getEnableAi()) {
            return;
        }

        predictorProperty.set(Optional.empty());

        BackgroundTask<Void> task = BackgroundTask
                .wrap(this::rebuild)
                .onSuccess(v -> {
                    LOGGER.info("Embedding model was successfully updated");
                    errorWhileBuildingModel = "";
                    eventBus.post(new EmbeddingModelBuiltEvent());
                })
                .onFailure(e -> {
                    LOGGER.error("An error occurred while building the embedding model", e);
                    dialogService.notify(Localization.lang("An error occurred while building the embedding model"));
                    errorWhileBuildingModel = e.getMessage();
                    eventBus.post(new EmbeddingModelBuildingErrorEvent());
                });
        task.titleProperty().set(Localization.lang("Updating local embedding model..."));
        task.showToUser(true);
        task.executeWith(taskExecutor);
    }

    public boolean isPresent() {
        return predictorProperty.get().isPresent();
    }

    public boolean hadErrorWhileBuildingModel() {
        return !errorWhileBuildingModel.isEmpty();
    }

    public String getErrorWhileBuildingModel() {
        return errorWhileBuildingModel;
    }

    private void rebuild() {
        if (!aiPreferences.getEnableAi()) {
            predictorProperty.set(Optional.empty());
            return;
        }

        LOGGER.info("Downloading embedding model...");

        String modelUrl = DJL_EMBEDDING_MODEL_URL_PREFIX + aiPreferences.getEmbeddingModel().getName();

        Criteria<String, float[]> criteria =
                Criteria.builder()
                        .setTypes(String.class, float[].class)
                        .optModelUrls(modelUrl)
                        .optEngine("PyTorch")
                        .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                        .optProgress(new ProgressBar())
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
    }

    private void setupListeningToPreferencesChanges() {
        aiPreferences.enableAiProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue && predictorProperty.get().isEmpty()) {
                startRebuildingTask();
            }
        });

        aiPreferences.customizeExpertSettingsProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue && predictorProperty.get().isEmpty()) {
                startRebuildingTask();
            }
        });

        aiPreferences.embeddingModelProperty().addListener(obs -> startRebuildingTask());
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> list) {
        if (predictorProperty.get().isEmpty()) {
            // The rationale for RuntimeException here:
            // 1. langchain4j error handling is a mess, and it uses RuntimeExceptions
            //    everywhere. Because this method implements a langchain4j interface,
            //    we follow the same "practice".
            // 2. There is no way to encode error information from type system: nor
            //    in the result type, nor "throws" in method signature. Actually,
            //    it's possible, but langchain4j doesn't do it.

            throw new RuntimeException(Localization.lang("Embedding model is not set up"));
        }

        return predictorProperty.get().get().embedAll(list);
    }

    @Override
    public void close() {
        executorService.shutdownNow();
        if (predictorProperty.get().isPresent()) {
            predictorProperty.get().get().close();
        }
    }
}
