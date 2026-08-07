package org.jabref.logic.ai.summarization.repositories;

import java.util.Optional;

import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.ai.summarization.AiSummaryIdentifier;

// [impl->req~ai.summarization.general.storage~1]
public interface SummariesRepository {
    void set(AiSummaryIdentifier summaryIdentifier, AiSummary aiSummary);

    Optional<AiSummary> get(AiSummaryIdentifier summaryIdentifier);

    void clear(AiSummaryIdentifier summaryIdentifier);
}
