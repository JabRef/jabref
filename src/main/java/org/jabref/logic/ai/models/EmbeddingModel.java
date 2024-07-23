package org.jabref.logic.ai.models;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.AiPreferences;

import ai.djl.MalformedModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.training.util.ProgressBar;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
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

    private final ObjectProperty<Optional<DeepJavaEmbeddingModel>> predictorProperty = new SimpleObjectProperty<>(Optional.empty());

    public EmbeddingModel(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;
        rebuild();
        setupListeningToPreferencesChanges();
    }

    private void rebuild() {
        if (!aiPreferences.getEnableChatWithFiles()) {
            predictorProperty.set(Optional.empty());
            return;
        }

        String modelUrl = "djl://ai.djl.huggingface.pytorch/sentence-transformers/" + aiPreferences.getEmbeddingModel().getLabel();

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
            throw new RuntimeException(Localization.lang("Unable to find the embedding model by the URL: %0", modelUrl), e);
        } catch (MalformedModelException e) {
            predictorProperty.set(Optional.empty());
            throw new RuntimeException(Localization.lang("The model by URL %0 is malformed", modelUrl), e);
        } catch (IOException e) {
            predictorProperty.set(Optional.empty());
            throw new RuntimeException(Localization.lang("An I/O error occurred while opening the embedding model by URL %0", modelUrl), e);
        }
    }

    private void setupListeningToPreferencesChanges() {
        aiPreferences.embeddingModelProperty().addListener(obs -> rebuild());
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

            throw new RuntimeException(Localization.lang("AI chat is not allowed"));
        }

        return predictorProperty.get().get().embedAll(list);
    }

    @Override
    public void close() throws Exception {
        executorService.shutdownNow();
        if (predictorProperty.get().isPresent()) {
            predictorProperty.get().get().close();
        }
    }
}
