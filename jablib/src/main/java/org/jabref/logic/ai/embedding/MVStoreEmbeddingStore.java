package org.jabref.logic.ai.embedding;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jabref.logic.ai.util.MVStoreBase;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.CosineSimilarity;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.RelevanceScore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import org.jspecify.annotations.Nullable;

import static java.util.Comparator.comparingDouble;
import static org.jabref.logic.ai.ingestion.logic.EmbeddingsCleaner.FILE_HASH_METADATA_KEY;

/// A custom implementation of langchain4j's [EmbeddingStore] that uses an MVStore as an embedded database.
///
/// Every embedding has 3 fields: float array (the embedding itself), file hash where it was generated from, and the embedded
/// string (the content).
///
public class MVStoreEmbeddingStore extends MVStoreBase implements EmbeddingStore<TextSegment> {

    private static final String FILE_HASH_MAP_NAME = "file-hashes";
    private static final String CONTENT_MAP_NAME = "contents";
    private static final String EMBEDDING_VECTOR_MAP_NAME = "embeddings";

    private final Map<String, String> fileHashMap;
    private final Map<String, String> contentMap;
    private final Map<String, float[]> embeddingVectorMap;

    public MVStoreEmbeddingStore(Path path, NotificationService dialogService) {
        super(path, dialogService);

        this.fileHashMap = this.mvStore.openMap(FILE_HASH_MAP_NAME);
        this.contentMap = this.mvStore.openMap(CONTENT_MAP_NAME);
        this.embeddingVectorMap = this.mvStore.openMap(EMBEDDING_VECTOR_MAP_NAME);
    }

    @Override
    public String add(Embedding embedding) {
        // Every embedding must have a unique id (convention in langchain4j.
        // Additionally, it is a key in the {@link Map}.
        // Most of the code in this class was borrowed from {@link InMemoryEmbeddingStore}.
        String id = String.valueOf(UUID.randomUUID());
        add(id, embedding);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        return embeddings.stream().map(this::add).toList();
    }

    @Override
    public void add(String id, Embedding embedding) {
        // It does not make much sense to store single embedding vector, but this is a requirement from langchain4j's
        // {@link EmbeddingStore}.
        fileHashMap.put(id, null);
        contentMap.put(id, "");
        embeddingVectorMap.put(id, embedding.vector());
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = String.valueOf(UUID.randomUUID());
        String fileHash = textSegment.metadata().getString(FILE_HASH_METADATA_KEY);
        fileHashMap.put(id, fileHash);
        contentMap.put(id, textSegment.text());
        embeddingVectorMap.put(id, embedding.vector());
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
        return IntStream.range(0, embeddings.size()).mapToObj(i -> add(embeddings.get(i), embedded.get(i))).toList();
    }

    @Override
    public void remove(String id) {
        fileHashMap.remove(id);
        contentMap.remove(id);
        embeddingVectorMap.remove(id);
    }

    @Override
    public void removeAll(Filter filter) {
        List<String> idsToRemove = applyFilter(filter).toList();
        idsToRemove.forEach(this::remove);
    }

    @Override
    public void removeAll() {
        fileHashMap.clear();
        contentMap.clear();
        embeddingVectorMap.clear();
    }

    /// The main function of finding most relevant text segments.
    /// Note: the only filters supported are:
    ///
    /// - [IsIn] with key [org.jabref.logic.ai.ingestion.logic.EmbeddingsCleaner#FILE_HASH_METADATA_KEY]
    /// - [IsEqualTo] with key [org.jabref.logic.ai.ingestion.logic.EmbeddingsCleaner#FILE_HASH_METADATA_KEY]
    ///
    /// @param request embedding search request
    /// @return an [EmbeddingSearchResult], which contains most relevant text segments
    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        // Source: {@link InMemoryEmbeddingStore}.

        Comparator<EmbeddingMatch<TextSegment>> comparator = comparingDouble(EmbeddingMatch::score);
        PriorityQueue<EmbeddingMatch<TextSegment>> matches = new PriorityQueue<>(comparator);

        applyFilter(request.filter()).forEach(id -> {
            float[] embeddingVector = embeddingVectorMap.getOrDefault(id, new float[0]);
            String content = contentMap.getOrDefault(id, "");
            String fileHash = fileHashMap.get(id);

            double cosineSimilarity = CosineSimilarity.between(Embedding.from(embeddingVector), request.queryEmbedding());
            double score = RelevanceScore.fromCosineSimilarity(cosineSimilarity);

            if (score >= request.minScore()) {
                matches.add(
                        new EmbeddingMatch<>(
                                score,
                                id,
                                Embedding.from(embeddingVector),
                                new TextSegment(
                                        content,
                                        new Metadata(
                                                fileHash == null ? Map.of() : Map.of(FILE_HASH_METADATA_KEY, fileHash)))));

                if (matches.size() > request.maxResults()) {
                    matches.poll();
                }
            }
        });

        List<EmbeddingMatch<TextSegment>> result = new ArrayList<>(matches);
        result.sort(comparator.reversed());

        return new EmbeddingSearchResult<>(result);
    }

    @Override
    public void removeAll(Collection ids) {
        ids.forEach(id -> {
            fileHashMap.remove(id);
            contentMap.remove(id);
            embeddingVectorMap.remove(id);
        });
    }

    private Stream<String> applyFilter(@Nullable Filter filter) {
        return switch (filter) {
            case null ->
                    fileHashMap.keySet().stream();

            case IsIn isInFilter when Objects.equals(isInFilter.key(), FILE_HASH_METADATA_KEY) ->
                    filterEntries(entry -> isInFilter.comparisonValues().contains(entry.getValue()));

            case IsEqualTo isEqualToFilter when Objects.equals(isEqualToFilter.key(), FILE_HASH_METADATA_KEY) ->
                    filterEntries(entry -> isEqualToFilter.comparisonValue().equals(entry.getValue()));

            default ->
                    throw new IllegalArgumentException("Wrong filter passed to MVStoreEmbeddingStore");
        };
    }

    private Stream<String> filterEntries(Predicate<Map.Entry<String, String>> predicate) {
        return fileHashMap.entrySet().stream().filter(predicate).map(Map.Entry::getKey);
    }

    @Override
    protected String errorMessageForOpening() {
        return "An error occurred while opening the embeddings cache file. Embeddings will not be stored in the next session.";
    }

    @Override
    protected String errorMessageForOpeningLocalized() {
        return Localization.lang("An error occurred while opening the embeddings cache file. Embeddings will not be stored in the next session.");
    }
}
