package org.jabref.logic.ai.ingestion;

import java.io.Serializable;
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

import org.jabref.gui.DialogService;
import org.jabref.logic.ai.util.MVStoreBase;
import org.jabref.logic.l10n.Localization;

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
import jakarta.annotation.Nullable;
import org.h2.mvstore.MVStore;

import static java.util.Comparator.comparingDouble;
import static org.jabref.logic.ai.ingestion.FileEmbeddingsManager.LINK_METADATA_KEY;

/**
 * A custom implementation of langchain4j's {@link EmbeddingStore} that uses a {@link MVStore} as an embedded database.
 * <p>
 * Every embedding has 3 fields: float array (the embedding itself), file where it was generated from, and the embedded
 * string (the content).
 * <p>
 */
public class MVStoreEmbeddingStore extends MVStoreBase implements EmbeddingStore<TextSegment> {
    // `file` field is nullable, because {@link Optional} can't be serialized.
    private record EmbeddingRecord(@Nullable String file, String content, float[] embeddingVector) implements Serializable { }

    private static final String EMBEDDINGS_MAP_NAME = "embeddings";

    private final Map<String, EmbeddingRecord> embeddingsMap;

    public MVStoreEmbeddingStore(Path path, DialogService dialogService) {
        super(path, dialogService);

        this.embeddingsMap = this.mvStore.openMap(EMBEDDINGS_MAP_NAME);
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
        embeddingsMap.put(id, new EmbeddingRecord(null, "", embedding.vector()));
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = String.valueOf(UUID.randomUUID());
        String linkedFile = textSegment.metadata().getString(LINK_METADATA_KEY);
        embeddingsMap.put(id, new EmbeddingRecord(linkedFile, textSegment.text(), embedding.vector()));
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
        return IntStream.range(0, embeddings.size()).mapToObj(i -> add(embeddings.get(i), embedded.get(i))).toList();
    }

    @Override
    public void remove(String id) {
        embeddingsMap.remove(id);
    }

    @Override
    public void removeAll(Filter filter) {
        List<String> idsToRemove = applyFilter(filter).toList();
        idsToRemove.forEach(this::remove);
    }

    @Override
    public void removeAll() {
        embeddingsMap.clear();
    }

    /**
     * The main function of finding most relevant text segments.
     * Note: the only filters supported are:
     * - {@link IsIn} with key {@link LINK_METADATA_KEY}
     * - {@link IsEqualTo} with key {@link LINK_METADATA_KEY}
     *
     * @param request embedding search request
     *
     * @return an {@link EmbeddingSearchResult}, which contains most relevant text segments
     */
    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        // Source: {@link InMemoryEmbeddingStore}.

        Comparator<EmbeddingMatch<TextSegment>> comparator = comparingDouble(EmbeddingMatch::score);
        PriorityQueue<EmbeddingMatch<TextSegment>> matches = new PriorityQueue<>(comparator);

        applyFilter(request.filter()).forEach(id -> {
            EmbeddingRecord eRecord = embeddingsMap.get(id);

            double cosineSimilarity = CosineSimilarity.between(Embedding.from(eRecord.embeddingVector), request.queryEmbedding());
            double score = RelevanceScore.fromCosineSimilarity(cosineSimilarity);

            if (score >= request.minScore()) {
                matches.add(
                        new EmbeddingMatch<>(
                                score,
                                id,
                                Embedding.from(eRecord.embeddingVector),
                                new TextSegment(
                                        eRecord.content,
                                        new Metadata(
                                                eRecord.file == null ? Map.of() : Map.of(LINK_METADATA_KEY, eRecord.file)))));

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
        embeddingsMap.entrySet().removeIf(entry -> ids.contains(entry.getKey()));
    }

    private Stream<String> applyFilter(@Nullable Filter filter) {
        return switch (filter) {
            case null -> embeddingsMap.keySet().stream();

            case IsIn isInFilter when Objects.equals(isInFilter.key(), LINK_METADATA_KEY) ->
                    filterEntries(entry -> isInFilter.comparisonValues().contains(entry.getValue().file));

            case IsEqualTo isEqualToFilter when Objects.equals(isEqualToFilter.key(), LINK_METADATA_KEY) ->
                    filterEntries(entry -> isEqualToFilter.comparisonValue().equals(entry.getValue().file));

            default -> throw new IllegalArgumentException("Wrong filter passed to MVStoreEmbeddingStore");
        };
    }

    private Stream<String> filterEntries(Predicate<Map.Entry<String, EmbeddingRecord>> predicate) {
        return embeddingsMap.entrySet().stream().filter(predicate).map(Map.Entry::getKey);
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
