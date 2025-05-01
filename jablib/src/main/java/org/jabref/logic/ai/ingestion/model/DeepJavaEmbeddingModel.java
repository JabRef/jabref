package org.jabref.logic.ai.ingestion.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

public class DeepJavaEmbeddingModel implements EmbeddingModel, AutoCloseable {
    private final ZooModel<String, float[]> model;
    private final Predictor<String, float[]> predictor;

    public DeepJavaEmbeddingModel(Criteria<String, float[]> criteria) throws ModelNotFoundException, MalformedModelException, IOException {
        this.model = criteria.loadModel();
        this.predictor = model.newPredictor();
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
}
