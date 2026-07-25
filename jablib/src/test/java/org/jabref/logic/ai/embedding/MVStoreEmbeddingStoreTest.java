package org.jabref.logic.ai.embedding;

import java.nio.file.Path;
import java.util.List;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.jabref.logic.ai.ingestion.logic.EmbeddingsCleaner.FILE_HASH_METADATA_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MVStoreEmbeddingStoreTest {

    @TempDir
    Path tempDir;

    private MVStoreEmbeddingStore store;

    @BeforeEach
    void setUp() {
        store = new MVStoreEmbeddingStore(tempDir.resolve("embeddings-test.mv"), _ -> {
        });
    }

    private static Embedding unitEmbedding(float... values) {
        return Embedding.from(values);
    }

    private static TextSegment segmentWithHash(String text, String hash) {
        return new TextSegment(text, new Metadata(java.util.Map.of(FILE_HASH_METADATA_KEY, hash)));
    }

    @Test
    void addAndSearchReturnsMatchingSegment() {
        Embedding embedding = unitEmbedding(1.0f, 0.0f, 0.0f);
        TextSegment segment = segmentWithHash("hello world", "hash-abc");
        store.add(embedding, segment);

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                                                               .queryEmbedding(embedding)
                                                               .maxResults(5)
                                                               .minScore(0.0)
                                                               .build();

        EmbeddingSearchResult<TextSegment> result = store.search(request);

        assertEquals(1, result.matches().size());
        assertEquals("hello world", result.matches().getFirst().embedded().text());
    }

    @Test
    void removeAllWithNoFilterClearsStore() {
        store.add(unitEmbedding(1.0f, 0.0f), segmentWithHash("seg1", "h1"));
        store.add(unitEmbedding(0.0f, 1.0f), segmentWithHash("seg2", "h2"));

        store.removeAll();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                                                               .queryEmbedding(unitEmbedding(1.0f, 0.0f))
                                                               .maxResults(10)
                                                               .minScore(0.0)
                                                               .build();

        assertTrue(store.search(request).matches().isEmpty());
    }

    @Test
    void removeAllWithIsEqualToFilterRemovesOnlyMatchingHash() {
        store.add(unitEmbedding(1.0f, 0.0f), segmentWithHash("keep", "hash-keep"));
        store.add(unitEmbedding(0.0f, 1.0f), segmentWithHash("remove", "hash-remove"));

        store.removeAll(MetadataFilterBuilder.metadataKey(FILE_HASH_METADATA_KEY).isEqualTo("hash-remove"));

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                                                               .queryEmbedding(unitEmbedding(1.0f, 0.0f))
                                                               .maxResults(10)
                                                               .minScore(0.0)
                                                               .build();

        List<EmbeddingMatch<TextSegment>> matches = store.search(request).matches();

        assertEquals(1, matches.size());
        assertEquals("keep", matches.getFirst().embedded().text());
    }

    @Test
    void searchWithIsEqualToFilterReturnsOnlyMatchingSegments() {
        store.add(unitEmbedding(1.0f, 0.0f), segmentWithHash("doc-A", "hash-A"));
        store.add(unitEmbedding(1.0f, 0.0f), segmentWithHash("doc-B", "hash-B"));

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                                                               .queryEmbedding(unitEmbedding(1.0f, 0.0f))
                                                               .maxResults(10)
                                                               .minScore(0.0)
                                                               .filter(MetadataFilterBuilder.metadataKey(FILE_HASH_METADATA_KEY).isEqualTo("hash-A"))
                                                               .build();

        List<EmbeddingMatch<TextSegment>> matches = store.search(request).matches();

        assertEquals(1, matches.size());
        assertEquals("doc-A", matches.getFirst().embedded().text());
    }

    @Test
    void searchWithIsInFilterReturnsAllMatchingHashes() {
        store.add(unitEmbedding(1.0f, 0.0f), segmentWithHash("doc-A", "hash-A"));
        store.add(unitEmbedding(1.0f, 0.0f), segmentWithHash("doc-B", "hash-B"));
        store.add(unitEmbedding(1.0f, 0.0f), segmentWithHash("doc-C", "hash-C"));

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                                                               .queryEmbedding(unitEmbedding(1.0f, 0.0f))
                                                               .maxResults(10)
                                                               .minScore(0.0)
                                                               .filter(MetadataFilterBuilder.metadataKey(FILE_HASH_METADATA_KEY).isIn("hash-A", "hash-B"))
                                                               .build();

        List<EmbeddingMatch<TextSegment>> matches = store.search(request).matches();

        assertEquals(2, matches.size());
    }

    @Test
    void removeSingleIdRemovesOnlyThatEntry() {
        String id1 = store.add(unitEmbedding(1.0f, 0.0f), segmentWithHash("keep", "h-keep"));
        store.add(unitEmbedding(0.0f, 1.0f), segmentWithHash("remove", "h-remove"));

        store.remove(id1);

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                                                               .queryEmbedding(unitEmbedding(0.0f, 1.0f))
                                                               .maxResults(10)
                                                               .minScore(0.0)
                                                               .build();

        List<EmbeddingMatch<TextSegment>> matches = store.search(request).matches();

        assertEquals(1, matches.size());
        assertEquals("remove", matches.getFirst().embedded().text());
    }
}
