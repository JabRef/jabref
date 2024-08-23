package org.jabref.logic.ai.ingestion.model;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.ai.AiPreferences;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around langchain4j {@link dev.langchain4j.model.embedding.EmbeddingModel}.
 * <p>
 * This class listens to preferences changes.
 */
public class JabRefEmbeddingModel implements EmbeddingModel, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefEmbeddingModel.class);

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

        new UpdateEmbeddingModelTask(aiPreferences, predictorProperty)
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
                })
                .executeWith(taskExecutor);
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
