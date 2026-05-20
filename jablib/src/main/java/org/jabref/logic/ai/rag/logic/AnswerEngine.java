package org.jabref.logic.ai.rag.logic;

import java.util.List;

import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.RelevantInformation;

// [impl->feat~ai.answer-engines~1]
public interface AnswerEngine {
    List<RelevantInformation> process(String query, List<FullBibEntry> entriesFilter);

    AnswerEngineKind getKind();
}
