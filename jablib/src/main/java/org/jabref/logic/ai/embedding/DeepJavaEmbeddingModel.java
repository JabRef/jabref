package org.jabref.logic.ai.embedding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.util.ProgressCounter;

import ai.djl.MalformedModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeepJavaEmbeddingModel implements EmbeddingModel, AutoCloseable {
    public static final String DJL_EMBEDDING_MODEL_URL_PREFIX = "djl://ai.djl.huggingface.pytorch/";

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepJavaEmbeddingModel.class);

    private final ZooModel<String, float[]> model;
    private final Predictor<String, float[]> predictor;

    public DeepJavaEmbeddingModel(
            String modelName,
            ProgressCounter progressCounter
    ) throws ModelNotFoundException, MalformedModelException, IOException {
        Criteria<String, float[]> criteria = makeCriteriaBuilder()
                .optModelUrls(DJL_EMBEDDING_MODEL_URL_PREFIX + modelName)
                .optProgress(progressCounter)
                .build();

        this.model = criteria.loadModel();
        this.predictor = model.newPredictor();
    }

    public static boolean isDownloaded(String modelName) {
        try {
            String modelUrl = DJL_EMBEDDING_MODEL_URL_PREFIX + modelName;
            return makeCriteria(modelUrl).isDownloaded();
        } catch (IOException | ModelNotFoundException e) {
            LOGGER.error("Got an error while checking if an embedding model is downloaded", e);
            return false;
        }
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> list) {
        try {
            List<Embedding> result = new ArrayList<>();

            for (TextSegment textSegment : list) {
                float[] embedding = predictor.predict(textSegment.text());
                result.add(new Embedding(embedding));
            }

            return new Response<>(result);
        } catch (TranslateException e) {
            // The rationale for RuntimeException here:
            // 1. langchain4j error handling is a mess, and it uses RuntimeExceptions
            //    everywhere. Because this method implements a langchain4j interface,
            //    we follow the same "practice".
            // 2. There is no way to encode error information from type system: nor
            //    in the result type, nor "throws" in method signature. Actually,
            //    it's possible, but langchain4j doesn't do it.

            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        this.model.close();
    }

    private static Criteria<String, float[]> makeCriteria(String modelUrl) {
        return makeCriteriaBuilder()
                .optModelUrls(modelUrl)
                .build();
    }

    private static Criteria.Builder<String, float[]> makeCriteriaBuilder() {
        return Criteria.builder()
                       .setTypes(String.class, float[].class)
                       .optEngine("PyTorch")
                       .optTranslatorFactory(new TextEmbeddingTranslatorFactory());
    }
}
