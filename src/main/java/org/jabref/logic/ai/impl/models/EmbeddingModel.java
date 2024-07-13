package org.jabref.logic.ai.impl.models;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.AiPreferences;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.output.Response;

/**
 * Wrapper around langchain4j embedding model.
 * <p>
 * This class listens to preferences changes.
 */
public class EmbeddingModel implements dev.langchain4j.model.embedding.EmbeddingModel, AutoCloseable {
    private final AiPreferences aiPreferences;

    private final ExecutorService executorService = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("ai-embedding-pool-%d").build()
    );

    private final ObjectProperty<Optional<dev.langchain4j.model.embedding.EmbeddingModel>> embeddingModelObjectProperty = new SimpleObjectProperty<>(Optional.empty());

    public EmbeddingModel(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;
        rebuild();
        setupListeningToPreferencesChanges();
    }

    private void rebuild() {
        if (!aiPreferences.getEnableChatWithFiles()) {
            embeddingModelObjectProperty.set(Optional.empty());
            return;
        }

        dev.langchain4j.model.embedding.EmbeddingModel embeddingModel = switch (aiPreferences.getEmbeddingModel()) {
            case AiPreferences.EmbeddingModel.ALL_MINLM_l6_V2 ->
                    new AllMiniLmL6V2EmbeddingModel(executorService);
            case AiPreferences.EmbeddingModel.ALL_MINLM_l6_V2_Q ->
                    new AllMiniLmL6V2QuantizedEmbeddingModel(executorService);
        };

        embeddingModelObjectProperty.set(Optional.of(embeddingModel));
    }

    private void setupListeningToPreferencesChanges() {
        aiPreferences.embeddingModelProperty().addListener(obs -> rebuild());
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> list) {
        if (embeddingModelObjectProperty.get().isEmpty()) {
            // The rationale for RuntimeException here:
            // 1. langchain4j error handling is a mess, and it uses RuntimeExceptions
            //    everywhere. Because this method implements a langchain4j interface,
            //    we follow the same "practice".
            // 2. There is no way to encode error information from type system: nor
            //    in the result type, nor "throws" in method signature. Actually,
            //    it's possible, but langchain4j doesn't do it.

            throw new RuntimeException(Localization.lang("AI chat is not allowed"));
        }

        return embeddingModelObjectProperty.get().get().embedAll(list);
    }

    @Override
    public void close() throws Exception {
        executorService.shutdownNow();
    }
}
