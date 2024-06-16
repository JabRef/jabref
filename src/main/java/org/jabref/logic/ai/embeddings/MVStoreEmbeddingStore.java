package org.jabref.logic.ai.embeddings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import static java.util.Comparator.comparingDouble;

public class MVStoreEmbeddingStore implements EmbeddingStore<TextSegment> {
    private final MVMap<String, float[]> embeddingsMap;
    private final MVMap<String, String> fileMap;
    private final MVMap<String, String> contentsMap;

    public MVStoreEmbeddingStore(MVStore mvStore) {
        // TODO: Will this work efficiently? Does optimizations work on map level or entry level?

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
        String id = String.valueOf(UUID.randomUUID());
        add(id, embedding);

        contentsMap.put(id, textSegment.text());

        String linkedFile = textSegment.metadata().getString("linkedFile");
        if (linkedFile != null) {
            fileMap.put(id, linkedFile);
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
        // Source: InMemoryEmbeddingStore.

        Comparator<EmbeddingMatch<TextSegment>> comparator = comparingDouble(EmbeddingMatch::score);
        PriorityQueue<EmbeddingMatch<TextSegment>> matches = new PriorityQueue<>(comparator);

        Filter filter = request.filter();

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
        result.sort(comparator);
        Collections.reverse(result);

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

            case IsIn isInFilter when Objects.equals(isInFilter.key(), "linkedFile") ->
                    filterEntries(entry -> isInFilter.comparisonValues().contains(entry.getValue()));

            case IsEqualTo isEqualToFilter when Objects.equals(isEqualToFilter.key(), "linkedFile") ->
                    filterEntries(entry -> isEqualToFilter.comparisonValue().equals(entry.getValue()));

            default -> throw new IllegalArgumentException("Wrong filter passed to MVStoreEmbeddingStore");
        };
    }

    private Stream<String> filterEntries(Predicate<Map.Entry<String, String>> predicate) {
        return fileMap.entrySet().stream().filter(predicate).map(Map.Entry::getKey);
    }
}
