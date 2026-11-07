package org.jabref.model.ai;

import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.pipeline.DocumentSplitterKind;
import org.jabref.model.ai.pipeline.ResponseEngineKind;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

public final class AiDefaultEnums {
    public static final AiProvider AI_PROVIDER = AiProvider.OPEN_AI;
    public static final SummarizatorKind SUMMARIZATOR_KIND = SummarizatorKind.CHUNKED;
    public static final TokenEstimatorKind TOKEN_ESTIMATOR_KIND = TokenEstimatorKind.MAX;
    public static final DocumentSplitterKind DOCUMENT_SPLITTER_KIND = DocumentSplitterKind.SLIDING_WINDOW;
    public static final ResponseEngineKind RESPONSE_ENGINE_KIND = ResponseEngineKind.EMBEDDINGS_SEARCH;

    private AiDefaultEnums() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }
}
