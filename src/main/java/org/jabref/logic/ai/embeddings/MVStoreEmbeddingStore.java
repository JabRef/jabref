package org.jabref.logic.ai.embeddings;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Comparator.comparingDouble;

/**
 * A custom implementation of langchain4j's {@link EmbeddingStore} that uses a {@link MVStore} as an embedded database.
 */
public class MVStoreEmbeddingStore implements EmbeddingStore<TextSegment> {
    public static final String LINKED_FILE_METADATA_KEY = "linkedFile";

    private static final Logger LOGGER = LoggerFactory.getLogger(MVStoreEmbeddingStore.class);

    private final Map<String, float[]> embeddingsMap;
    private final Map<String, String> fileMap;
    private final Map<String, String> contentsMap;

    public MVStoreEmbeddingStore(MVStore mvStore) {
        this.embeddingsMap = mvStore.openMap("embeddingsMap");
        this.fileMap = mvStore.openMap("fileMap");
        this.contentsMap = mvStore.openMap("contentsMap");
    }

    @Override
    public String add(Embedding embedding) {
        String id = String.valueOf(UUID.randomUUID());
        add(id, embedding);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        embeddingsMap.put(id, embedding.vector());
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        // Every embedding must have a unique id (conventions in `langchain4j`).
        // Most of the code in this class was borrowed from {@link InMemoryEmbeddingStore}.
        String id = String.valueOf(UUID.randomUUID());

        add(id, embedding);

        contentsMap.put(id, textSegment.text());

        String linkedFile = textSegment.metadata().getString(LINKED_FILE_METADATA_KEY);
        if (linkedFile != null) {
            fileMap.put(id, linkedFile);
        } else {
            LOGGER.debug("MVStoreEmbeddingStore got an embedding without a 'linkedFile' metadata entry. This embedding will be filtered out in AI chats.");
        }

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
        contentsMap.remove(id);
        fileMap.remove(id);
    }

    @Override
    public void removeAll(Filter filter) {
        List<String> idsToRemove = applyFilter(filter).toList();
        idsToRemove.forEach(this::remove);
    }

    @Override
    public void removeAll() {
        embeddingsMap.clear();
        contentsMap.clear();
        fileMap.clear();
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        // Source: {@link InMemoryEmbeddingStore}.

        Comparator<EmbeddingMatch<TextSegment>> comparator = comparingDouble(EmbeddingMatch::score);
        PriorityQueue<EmbeddingMatch<TextSegment>> matches = new PriorityQueue<>(comparator);

        applyFilter(request.filter()).forEach(id -> {
            Embedding embedding = new Embedding(embeddingsMap.get(id));

            double cosineSimilarity = CosineSimilarity.between(embedding, request.queryEmbedding());
            double score = RelevanceScore.fromCosineSimilarity(cosineSimilarity);

            if (score >= request.minScore()) {
                matches.add(new EmbeddingMatch<>(score, id, embedding, new TextSegment(contentsMap.get(id), new Metadata())));

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
        contentsMap.entrySet().removeIf(entry -> ids.contains(entry.getKey()));
        fileMap.entrySet().removeIf(entry -> ids.contains(entry.getKey()));
    }

    private Stream<String> applyFilter(@Nullable Filter filter) {
        return switch (filter) {
            case null -> embeddingsMap.keySet().stream();

            case IsIn isInFilter when Objects.equals(isInFilter.key(), LINKED_FILE_METADATA_KEY) ->
                    filterEntries(entry -> isInFilter.comparisonValues().contains(entry.getValue()));

            case IsEqualTo isEqualToFilter when Objects.equals(isEqualToFilter.key(), LINKED_FILE_METADATA_KEY) ->
                    filterEntries(entry -> isEqualToFilter.comparisonValue().equals(entry.getValue()));

            default -> throw new IllegalArgumentException("Wrong filter passed to MVStoreEmbeddingStore");
        };
    }

    private Stream<String> filterEntries(Predicate<Map.Entry<String, String>> predicate) {
        return fileMap.entrySet().stream().filter(predicate).map(Map.Entry::getKey);
    }
}
