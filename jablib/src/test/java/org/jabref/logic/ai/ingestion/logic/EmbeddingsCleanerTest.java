package org.jabref.logic.ai.ingestion.logic;

import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.ai.embedding.MVStoreEmbeddingStore;
import org.jabref.logic.ai.ingestion.repositories.MVStoreIngestedDocumentsRepository;
import org.jabref.logic.ai.preferences.AiPreferences;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.jabref.logic.ai.ingestion.logic.EmbeddingsCleaner.FILE_HASH_METADATA_KEY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmbeddingsCleanerTest {

    @TempDir
    Path tempDir;

    private MVStoreEmbeddingStore embeddingStore;
    private MVStoreIngestedDocumentsRepository ingestedDocumentsRepository;
    private EmbeddingsCleaner cleaner;

    @BeforeEach
    void setUp() {
        embeddingStore = new MVStoreEmbeddingStore(tempDir.resolve("embeddings.mv"), _ -> {
        });
        ingestedDocumentsRepository = new MVStoreIngestedDocumentsRepository(_ -> {
        }, tempDir.resolve("ingested.mv"));

        AiPreferences aiPreferences = mock(AiPreferences.class);
        when(aiPreferences.getEmbeddingsProperties()).thenReturn(List.of());

        cleaner = new EmbeddingsCleaner(aiPreferences, embeddingStore, ingestedDocumentsRepository);
    }

    private static TextSegment segmentWithHash(String text, String hash) {
        return new TextSegment(text, new Metadata(java.util.Map.of(FILE_HASH_METADATA_KEY, hash)));
    }

    private boolean hasAnyEmbedding() {
        return !embeddingStore.search(EmbeddingSearchRequest.builder()
                                                            .queryEmbedding(Embedding.from(new float[] {1.0f, 0.0f}))
                                                            .maxResults(100)
                                                            .minScore(0.0)
                                                            .build()).matches().isEmpty();
    }

    @Test
    void removeAllClearsEmbeddingStore() {
        embeddingStore.add(Embedding.from(new float[] {1.0f, 0.0f}), segmentWithHash("doc", "hash-1"));

        cleaner.removeAll();

        assertFalse(hasAnyEmbedding());
    }

    @Test
    void removeAllClearsIngestedDocumentsRepository() {
        ingestedDocumentsRepository.markDocumentAsFullyIngested("hash-1");
        ingestedDocumentsRepository.markDocumentAsFullyIngested("hash-2");

        cleaner.removeAll();

        assertFalse(ingestedDocumentsRepository.isDocumentIngested("hash-1"));
        assertFalse(ingestedDocumentsRepository.isDocumentIngested("hash-2"));
    }

    @Test
    void removeDocumentDeletesOnlyItsEmbeddings() {
        embeddingStore.add(Embedding.from(new float[] {1.0f, 0.0f}), segmentWithHash("to remove", "hash-remove"));
        embeddingStore.add(Embedding.from(new float[] {0.0f, 1.0f}), segmentWithHash("to keep", "hash-keep"));

        cleaner.removeDocument("hash-remove");

        EmbeddingSearchRequest request = EmbeddingSearchRequest
                .builder()
                .queryEmbedding(Embedding.from(new float[] {0.0f, 1.0f}))
                .maxResults(10)
                .minScore(0.0)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        assertFalse(result.matches().isEmpty());
        assertTrue(result.matches().stream().allMatch(m -> "to keep".equals(m.embedded().text())));
    }

    @Test
    void removeDocumentUnmarksItInIngestedRepository() {
        ingestedDocumentsRepository.markDocumentAsFullyIngested("hash-abc");

        cleaner.removeDocument("hash-abc");

        assertFalse(ingestedDocumentsRepository.isDocumentIngested("hash-abc"));
    }

    @Test
    void removeDocumentDoesNotAffectOtherIngestedEntries() {
        ingestedDocumentsRepository.markDocumentAsFullyIngested("hash-abc");
        ingestedDocumentsRepository.markDocumentAsFullyIngested("hash-xyz");

        cleaner.removeDocument("hash-abc");

        assertTrue(ingestedDocumentsRepository.isDocumentIngested("hash-xyz"));
    }
}
