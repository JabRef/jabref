package org.jabref.logic.ai.embeddings;

import javafx.beans.property.BooleanProperty;

import org.jabref.preferences.AiPreferences;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;

public class LowLevelIngestor {
    private final AiPreferences aiPreferences;

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    private EmbeddingStoreIngestor ingestor;
    private DocumentSplitter documentSplitter;

    public LowLevelIngestor(AiPreferences aiPreferences, EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        this.aiPreferences = aiPreferences;
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;

        rebuild();

        setupListeningToPreferencesChanges();
    }

    private void rebuild() {
        this.documentSplitter = DocumentSplitters
                .recursive(aiPreferences.getDocumentSplitterChunkSize(),
                           aiPreferences.getDocumentSplitterOverlapSize());

        this.ingestor = EmbeddingStoreIngestor
                .builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .documentSplitter(documentSplitter)
                .build();
    }

    private void setupListeningToPreferencesChanges() {
        aiPreferences.onEmbeddingsParametersChange(this::rebuild);
    }

    /**
     * Add document to embedding store.
     * This method does not check if file was already ingested.
     *
     * @param document - document to add.
     * @param stopProperty - in case you want to stop the ingestion process, set this property to true.
     */
    public void ingestDocument(Document document, BooleanProperty stopProperty) {
        for (TextSegment documentPart : documentSplitter.split(document)) {
            if (stopProperty.get()) {
                return;
            }

            ingestor.ingest(new Document(documentPart.text(), document.metadata()));
        }
    }
}
