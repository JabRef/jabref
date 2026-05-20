package org.jabref.logic.ai.preferences;

import org.jabref.model.ai.embeddings.PredefinedEmbeddingModel;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.DocumentSplitterKind;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

/// A collection of values for the default settings of AI in the expert section.
///
/// This collection was made because "Expert settings" in the AI settings is resettable.
/// There are facilities in JabRef codebase to reset either all settings or 1 section, but not a part of a section.
public class AiDefaultExpertSettings {
    public static final PredefinedEmbeddingModel EMBEDDING_MODEL = PredefinedEmbeddingModel.SENTENCE_TRANSFORMERS_ALL_MINILM_L12_V2;
    public static final SummarizatorKind SUMMARIZATOR_KIND = SummarizatorKind.CHUNKED;
    public static final TokenEstimatorKind TOKEN_ESTIMATOR_KIND = TokenEstimatorKind.MAX;
    public static final float TEMPERATURE = 0.7F;
    public static final int CONTEXT_WINDOW_SIZE = 8192;

    public static final DocumentSplitterKind DOCUMENT_SPLITTER_KIND = DocumentSplitterKind.SLIDING_WINDOW;
    public static final int DOCUMENT_SPLITTER_CHUNK_SIZE = 300;
    public static final int DOCUMENT_SPLITTER_OVERLAP_SIZE = 100;

    public static final AnswerEngineKind ANSWER_ENGINE_KIND = AnswerEngineKind.EMBEDDINGS_SEARCH;
    public static final int RAG_MAX_RESULTS_COUNT = 10;
    public static final float RAG_MIN_SCORE = 0.3F;
}
