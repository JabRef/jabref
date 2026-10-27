package org.jabref.logic.ai.rag.logic;

import java.util.List;

import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.pipeline.RelevantInformation;
import org.jabref.model.ai.pipeline.ResponseEngineKind;

import org.jspecify.annotations.NullMarked;

// [impl->feat~ai.response-engines~1]
@NullMarked
public interface ResponseEngine {
    List<RelevantInformation> process(String query, List<FullBibEntry> entriesFilter);

    ResponseEngineKind getKind();
}

