package org.jabref.logic.ai.ingestion.logic.ingestion;

import java.util.List;

import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;

public class TextIngestor {
    private final EmbeddingStoreIngestor ingestor;
    private final DocumentSplitter documentSplitter;

    public TextIngestor(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            DocumentSplitter documentSplitter
    ) {
        this.ingestor = EmbeddingStoreIngestor
                .builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .documentSplitter(document -> List.of(new TextSegment(document.text(), document.metadata())))
                .build();

        this.documentSplitter = documentSplitter;
    }

    public void ingest(Metadata metadata, String text) throws InterruptedException {
        List<String> chunks = documentSplitter.split(text).toList();

        for (String documentPart : chunks) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            ingestor.ingest(new DefaultDocument(documentPart, metadata));
        }
    }
}
