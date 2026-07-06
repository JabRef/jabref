package org.jabref.logic.ai.rag.logic;

import java.util.List;

import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.pipeline.RelevantInformation;
import org.jabref.model.ai.pipeline.ResponseEngineKind;

// [impl->feat~ai.answer-engines~1]
public interface ResponseEngine {
    List<RelevantInformation> process(String query, List<FullBibEntry> entriesFilter);

    ResponseEngineKind getKind();
}


