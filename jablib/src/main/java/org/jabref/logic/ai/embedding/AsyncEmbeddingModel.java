package org.jabref.logic.ai.embedding;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.ai.ingestion.tasks.UpdateEmbeddingModelTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.embeddings.PredefinedEmbeddingModel;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// An {@link EmbeddingModel} that downloads/loads its underlying model asynchronously via a background task.
///
/// The constructor immediately schedules an {@link UpdateEmbeddingModelTask}. Call
/// {@link #embedAll(List)} only once the model is ready (check {@link #isPresent()}).
///
/// Does not listen to any preferences; the owner is responsible for reacting to preference
/// changes and creating a new instance if needed.
///
/// Implements {@link AutoCloseable}; close it to release the loaded model.
public class AsyncEmbeddingModel implements EmbeddingModel, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncEmbeddingModel.class);

    private final PredefinedEmbeddingModel embeddingModelKind;
    private final NotificationService notificationService;
    private final TaskExecutor taskExecutor;

    private final ObjectProperty<Optional<DeepJavaEmbeddingModel>> predictorProperty = new SimpleObjectProperty<>(Optional.empty());

    // Empty if there is no error.
    private String errorWhileBuildingModel = "";

    public AsyncEmbeddingModel(
            PredefinedEmbeddingModel embeddingModelKind,
            NotificationService notificationService,
            TaskExecutor taskExecutor
    ) {
        this.embeddingModelKind = embeddingModelKind;
        this.notificationService = notificationService;
        this.taskExecutor = taskExecutor;

        startRebuildingTask();
    }

    public void startRebuildingTask() {
        predictorProperty.set(Optional.empty());

        if (DeepJavaEmbeddingModel.isDownloaded(embeddingModelKind.getName())) {
            try {
                predictorProperty.set(Optional.of(new DeepJavaEmbeddingModel(embeddingModelKind.getName(), new ProgressCounter())));
            } catch (Exception e) {
                LOGGER.error("An error occurred while loading the embedding model", e);
                notificationService.notify(Localization.lang("An error occurred while loading the embedding model"));
                predictorProperty.set(Optional.empty());
                errorWhileBuildingModel = e.getMessage() == null ? "" : e.getMessage();
            }
        } else {
            new UpdateEmbeddingModelTask(embeddingModelKind.getName())
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

    @Override
    public Response<@NotNull List<Embedding>> embedAll(List<TextSegment> list) {
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
