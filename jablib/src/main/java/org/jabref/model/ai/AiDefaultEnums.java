package org.jabref.model.ai;

import org.jabref.model.ai.embeddings.PredefinedEmbeddingModel;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.DocumentSplitterKind;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

public final class AiDefaultEnums {
    public static final AiProvider AI_PROVIDER = AiProvider.OPEN_AI;
    public static final SummarizatorKind SUMMARIZATOR_KIND = SummarizatorKind.CHUNKED;
    public static final TokenEstimatorKind TOKEN_ESTIMATOR_KIND = TokenEstimatorKind.MAX;
    public static final PredefinedEmbeddingModel EMBEDDING_MODEL = PredefinedEmbeddingModel.SENTENCE_TRANSFORMERS_ALL_MINILM_L12_V2;
    public static final DocumentSplitterKind DOCUMENT_SPLITTER_KIND = DocumentSplitterKind.SLIDING_WINDOW;
    public static final AnswerEngineKind ANSWER_ENGINE_KIND = AnswerEngineKind.EMBEDDINGS_SEARCH;

    private AiDefaultEnums() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }
}
