package org.jabref.logic.ai.ingestion.logic.ingestion;

import java.util.List;

import org.jabref.logic.ai.embedding.EmbeddingInputPrefixes;
import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class TextIngestor {
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final DocumentSplitter documentSplitter;

    public TextIngestor(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            DocumentSplitter documentSplitter
    ) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.documentSplitter = documentSplitter;
    }

    public void ingest(Metadata metadata, String text) throws InterruptedException {
        List<String> chunks = documentSplitter.split(text).toList();

        for (String documentPart : chunks) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            // The prefix only steers the embedding; the stored text stays as in the document
            embeddingStore.add(
                    embeddingModel.embed(EmbeddingInputPrefixes.forPassage(embeddingModel, documentPart)).content(),
                    TextSegment.from(documentPart, metadata.copy()));
        }
    }
}
