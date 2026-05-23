package org.jabref.logic.ai.summarization.util;

import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.ChunkedSummarizator;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.FullDocumentSummarizator;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.model.ai.summarization.SummarizatorKind;

public final class SummarizatorFactory {
    private SummarizatorFactory() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    public static Summarizator create(
            SummarizatorKind summarizatorKind,
            String summarizationChunkSystemMessageTemplate,
            String summarizationCombineSystemMessageTemplate,
            String summarizationFullDocumentSystemMessageTemplate
    ) {
        return switch (summarizatorKind) {
            case SummarizatorKind.CHUNKED ->
                    new ChunkedSummarizator(
                            summarizationChunkSystemMessageTemplate,
                            summarizationCombineSystemMessageTemplate
                    );

            case SummarizatorKind.FULL_DOCUMENT ->
                    new FullDocumentSummarizator(
                            summarizationFullDocumentSystemMessageTemplate
                    );
        };
    }

    /// Convenience overload that reads all parameters from {@link AiPreferences}.
    public static Summarizator create(AiPreferences aiPreferences) {
        return create(
                aiPreferences.getSummarizatorKind(),
                aiPreferences.getSummarizationChunkSystemMessageTemplate(),
                aiPreferences.getSummarizationCombineSystemMessageTemplate(),
                aiPreferences.getSummarizationFullDocumentSystemMessageTemplate()
        );
    }
}
