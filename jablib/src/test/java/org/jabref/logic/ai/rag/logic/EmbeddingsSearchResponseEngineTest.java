package org.jabref.logic.ai.rag.logic;

import java.util.List;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.ingestion.logic.ingestion.FileIngestor;
import org.jabref.model.ai.pipeline.RelevantInformation;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class EmbeddingsSearchResponseEngineTest {

    private EmbeddingModel embeddingModel;
    private EmbeddingStore<TextSegment> embeddingStore;
    private EmbeddingsSearchResponseEngine responseEngine;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        FilePreferences filePreferences = mock(FilePreferences.class);
        embeddingModel = mock(EmbeddingModel.class);
        embeddingStore = (EmbeddingStore<TextSegment>) mock(EmbeddingStore.class);

        when(embeddingModel.embed(any(String.class))).thenReturn(Response.from(new Embedding(new float[] {0.1f, 0.2f})));

        responseEngine = new EmbeddingsSearchResponseEngine(
                filePreferences,
                embeddingModel,
                embeddingStore,
                0.7,
                10
        );
    }

    @Test
    void processExtractsPageNumberFromMetadata() {
        Metadata metadata = new Metadata();
        metadata.put(FileIngestor.PAGE_NUMBER_METADATA_KEY, 7);

        TextSegment textSegment = new TextSegment("Sample excerpt text", metadata);
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.85, "match-1", new Embedding(new float[] {0.1f, 0.2f}), textSegment);
        EmbeddingSearchResult<TextSegment> searchResult = new EmbeddingSearchResult<>(List.of(match));

        when(embeddingStore.search(any(EmbeddingSearchRequest.class))).thenReturn(searchResult);

        List<RelevantInformation> result = responseEngine.process("sample query", List.of());

        assertEquals(1, result.size());
        RelevantInformation info = result.getFirst();
        assertEquals(7, info.pageNumber());
        assertEquals("Sample excerpt text", info.text());
    }

    @Test
    void processHandlesMissingPageNumberInMetadata() {
        Metadata metadata = new Metadata();

        TextSegment textSegment = new TextSegment("Text without page number", metadata);
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.85, "match-2", new Embedding(new float[] {0.1f, 0.2f}), textSegment);
        EmbeddingSearchResult<TextSegment> searchResult = new EmbeddingSearchResult<>(List.of(match));

        when(embeddingStore.search(any(EmbeddingSearchRequest.class))).thenReturn(searchResult);

        List<RelevantInformation> result = responseEngine.process("sample query", List.of());

        assertEquals(1, result.size());
        RelevantInformation info = result.getFirst();
        assertNull(info.pageNumber());
        assertEquals("Text without page number", info.text());
    }
}
