package org.jabref.logic.ai.embedding;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.ai.ingestion.tasks.UpdateEmbeddingModelTask;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// An [EmbeddingModel] that downloads/loads its underlying model asynchronously via a background task.
///
/// The constructor immediately schedules an [UpdateEmbeddingModelTask]. Call
/// [#embedAll(List)] only once the model is ready (check [#isPresent()]).
///
/// Does not listen to any preferences; the owner is responsible for reacting to preference
/// changes and creating a new instance if needed.
///
/// Implements [AutoCloseable]; close it to release the loaded model.
public class AsyncEmbeddingModel implements EmbeddingModel, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncEmbeddingModel.class);

    private final String modelName;
    private final NotificationService notificationService;
    private final TaskExecutor taskExecutor;
    private final EmbeddingModelMetadataService metadataService;

    private final ObjectProperty<Optional<DeepJavaEmbeddingModel>> predictorProperty = new SimpleObjectProperty<>(Optional.empty());

    // Empty if there is no error.
    private String errorWhileBuildingModel = "";

    public AsyncEmbeddingModel(
            String modelName,
            AiPreferences aiPreferences,
            NotificationService notificationService,
            TaskExecutor taskExecutor,
            EmbeddingModelMetadataService metadataService
    ) {
        this.modelName = modelName;
        this.notificationService = notificationService;
        this.taskExecutor = taskExecutor;
        this.metadataService = metadataService;

        if (aiPreferences.getAiFeaturesEnabled()) {
            startRebuildingTask();
        }
    }

    public void startRebuildingTask() {
        predictorProperty.set(Optional.empty());

        new UpdateEmbeddingModelTask(modelName, metadataService)
                .onSuccess(model -> {
                    predictorProperty.set(Optional.of(model));
                    errorWhileBuildingModel = "";
                })
                .onFailure(e -> {
                    LOGGER.error("An error occurred while downloading the embedding model", e);
                    notificationService.notify(Localization.lang("An error occurred while downloading the embedding model"));
                    predictorProperty.set(Optional.empty());
                    errorWhileBuildingModel = e.getMessage() == null ? "" : e.getMessage();
                })
                .executeWith(taskExecutor);
    }

    public boolean isPresent() {
        return predictorProperty.get().isPresent();
    }

    public String getModelName() {
        return modelName;
    }

    public OptionalInt getMaxSnippetTokens() {
        return predictorProperty.get()
                .map(DeepJavaEmbeddingModel::getMaxSnippetTokens)
                .orElseGet(() -> metadataService
                        .getMetadata(modelName)
                        .map(EmbeddingModelMetadata::maxSnippetTokens)
                        .orElseGet(OptionalInt::empty));
    }

    public boolean hadErrorWhileBuildingModel() {
        return !errorWhileBuildingModel.isEmpty();
    }

    public String getErrorWhileBuildingModel() {
        return errorWhileBuildingModel;
    }

    @Override
    public Response<@NonNull List<Embedding>> embedAll(List<TextSegment> list) {
        if (predictorProperty.get().isEmpty()) {
            // The rationale for RuntimeException here:
            // 1. langchain4j error handling is a mess, and it uses RuntimeExceptions
            //    everywhere. Because this method implements a langchain4j interface,
            //    we follow the same "practice".
            // 2. There is no way to encode error information from the type system: nor
            //    in the result type, nor "throws" in the method signature.
            throw new RuntimeException(Localization.lang("Embedding model is not set up"));
        }

        return predictorProperty.get().get().embedAll(list);
    }

    @Override
    public void close() {
        if (predictorProperty.get().isPresent()) {
            predictorProperty.get().get().close();
        }
    }
}
