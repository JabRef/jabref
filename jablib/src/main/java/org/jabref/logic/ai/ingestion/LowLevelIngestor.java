package org.jabref.logic.ai.ingestion;

import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.ai.AiPreferences;

import dev.langchain4j.data.document.DefaultDocument;
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
        aiPreferences.customizeExpertSettingsProperty().addListener(_ -> rebuild());
        aiPreferences.addListenerToEmbeddingsParametersChange(this::rebuild);
    }

    /**
     * Add document to embedding store.
     * This method does not check if file was already ingested.
     *
     * @param document     - document to add.
     * @param stopProperty - in case you want to stop the ingestion process, set this property to true.
     */
    public void ingestDocument(Document document, ReadOnlyBooleanProperty stopProperty, IntegerProperty workDone, IntegerProperty workMax) throws InterruptedException {
        List<TextSegment> textSegments = documentSplitter.split(document);
        workMax.set(textSegments.size());

        for (TextSegment documentPart : textSegments) {
            if (stopProperty.get()) {
                throw new InterruptedException();
            }

            ingestor.ingest(new DefaultDocument(documentPart.text(), document.metadata()));

            workDone.set(workDone.get() + 1);
        }
    }
}
