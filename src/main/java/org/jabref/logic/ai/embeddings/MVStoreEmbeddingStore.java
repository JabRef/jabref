package org.jabref.logic.ai.embeddings;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jabref.gui.DialogService;
import org.jabref.logic.ai.FileEmbeddingsManager;

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
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Comparator.comparingDouble;

/**
 * A custom implementation of langchain4j's {@link EmbeddingStore} that uses a {@link MVStore} as an embedded database.
 * <p>
 * Every embedding has 3 fields: float array (the embedding itself), file where it was generated from, and the embedded
 * string (the content). Each of those fields is stored in a separate {@link MVMap}.
 * To connect values in those fields we use an id, which is a random {@link UUID}.
 * <p>
 */
public class MVStoreEmbeddingStore implements EmbeddingStore<TextSegment>, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MVStoreEmbeddingStore.class);

    private final MVStore mvStore;

    // `file` field is nullable, because {@link Optional} can't be serialized.
    private record EmbeddingRecord(
            @Nullable String file, String content, float[] embeddingVector) implements Serializable {}

    private final Map<String, EmbeddingRecord> embeddingsMap;

    /**
     * Construct an embedding store that uses an MVStore for persistence.
     *
     * @param path - path to MVStore file, if null then in-memory MVStore will be used (without persistence).
     * @param dialogService - dialog service that is used in case any error happens while opening an MVStore.
     */
    public MVStoreEmbeddingStore(@Nullable Path path, DialogService dialogService) {
        MVStore mvStoreTemp;
        try {
            mvStoreTemp = MVStore.open(path == null ? null : path.toString());
        } catch (Exception e) {
            dialogService.showErrorDialogAndWait("Unable to open embeddings cache file. Will store cache in RAM", e);
            mvStoreTemp = MVStore.open(null);
        }

        this.mvStore = mvStoreTemp;
        this.embeddingsMap = mvStore.openMap("embeddingsMap");
    }

    @Override
    public String add(Embedding embedding) {
        // Every embedding must have a unique id (conventions in `langchain4j` + it's a key to the {@link Map}.
        // Most of the code in this class was borrowed from {@link InMemoryEmbeddingStore}.
        String id = String.valueOf(UUID.randomUUID());
        add(id, embedding);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        // It doesn't make much sense to store single embedding vector, but this is a requirement from `langchain4j`
        // {@link EmbeddingStore}.
        embeddingsMap.put(id, new EmbeddingRecord(null, "", embedding.vector()));
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = String.valueOf(UUID.randomUUID());

        String linkedFile = textSegment.metadata().getString(FileEmbeddingsManager.LINK_METADATA_KEY);

        embeddingsMap.put(id, new EmbeddingRecord(linkedFile, textSegment.text(), embedding.vector()));

        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        return embeddings.stream().map(this::add).toList();
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
     * - {@link IsIn} with key {@link FileEmbeddingsManager.LINK_METADATA_KEY}
     * - {@link IsEqualTo} with key {@link FileEmbeddingsManager.LINK_METADATA_KEY}
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
            EmbeddingRecord record = embeddingsMap.get(id);

            double cosineSimilarity = CosineSimilarity.between(Embedding.from(record.embeddingVector), request.queryEmbedding());
            double score = RelevanceScore.fromCosineSimilarity(cosineSimilarity);

            if (score >= request.minScore()) {
                matches.add(new EmbeddingMatch<>(score, id, Embedding.from(record.embeddingVector), new TextSegment(record.content, new Metadata())));

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

            case IsIn isInFilter when Objects.equals(isInFilter.key(), FileEmbeddingsManager.LINK_METADATA_KEY) ->
                    filterEntries(entry -> isInFilter.comparisonValues().contains(entry.getValue().file));

            case IsEqualTo isEqualToFilter when Objects.equals(isEqualToFilter.key(), FileEmbeddingsManager.LINK_METADATA_KEY) ->
                    filterEntries(entry -> isEqualToFilter.comparisonValue().equals(entry.getValue().file));

            default -> throw new IllegalArgumentException("Wrong filter passed to MVStoreEmbeddingStore");
        };
    }

    private Stream<String> filterEntries(Predicate<Map.Entry<String, EmbeddingRecord>> predicate) {
        return embeddingsMap.entrySet().stream().filter(predicate).map(Map.Entry::getKey);
    }

    @Override
    public void close() {
        mvStore.close();
    }
}
