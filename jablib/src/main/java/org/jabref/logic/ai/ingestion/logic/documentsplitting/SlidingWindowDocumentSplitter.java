package org.jabref.logic.ai.ingestion.logic.documentsplitting;

import java.util.stream.Stream;

import org.jabref.model.ai.pipeline.DocumentSplitterKind;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

public class SlidingWindowDocumentSplitter implements DocumentSplitter {
    private final dev.langchain4j.data.document.DocumentSplitter langchainDocumentSplitter;

    public SlidingWindowDocumentSplitter(int chunkSize, int chunkOverlap) {
        this.langchainDocumentSplitter = DocumentSplitters.recursive(
                chunkSize,
                chunkOverlap
        );
    }

    @Override
    public Stream<String> split(String text) {
        return langchainDocumentSplitter
                .split(new DefaultDocument(text))
                .stream()
                .map(TextSegment::text);
    }

    @Override
    public DocumentSplitterKind getKind() {
        return DocumentSplitterKind.SLIDING_WINDOW;
    }
}
