package org.jabref.model.ai.summarization;

import org.jabref.model.ai.AiMetadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

// [model->feat~ai.summarization~1]
public record AiSummary(
        AiMetadata metadata,
        SummarizatorKind summarizationAlgorithm,
        String content
) {
    @JsonCreator()
    public AiSummary(
            @JsonProperty("metadata") AiMetadata metadata,
            @JsonProperty("summarizationAlgorithm") SummarizatorKind summarizationAlgorithm,
            @JsonProperty("content") String content
    ) {
        this.metadata = metadata;
        this.summarizationAlgorithm = summarizationAlgorithm;
        this.content = content;
    }
}
