package org.jabref.logic.ai.ingestion.logic.documentsplitting;

import java.util.stream.Stream;

import org.jabref.model.ai.pipeline.DocumentSplitterKind;

public interface DocumentSplitter {
    Stream<String> split(String text) throws InterruptedException;

    DocumentSplitterKind getKind();
}
