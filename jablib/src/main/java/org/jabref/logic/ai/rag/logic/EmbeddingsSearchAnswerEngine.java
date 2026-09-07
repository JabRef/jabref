package org.jabref.logic.ai.rag.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.ingestion.logic.EmbeddingsCleaner;
import org.jabref.logic.ai.ingestion.util.FileHasher;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.RelevantInformation;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// [impl->feat~ai.answer-engines.embeddings-search~1]
public class EmbeddingsSearchAnswerEngine implements AnswerEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingsSearchAnswerEngine.class);

    private final FilePreferences filePreferences;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final double minimumScore;
    private final int maximumResultsCount;

    public EmbeddingsSearchAnswerEngine(
            FilePreferences filePreferences,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            double minimumScore,
            int maximumResultsCount
    ) {
        this.filePreferences = filePreferences;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.minimumScore = minimumScore;
        this.maximumResultsCount = maximumResultsCount;
    }

    @Override
    public List<RelevantInformation> process(
            String query,
            List<FullBibEntry> entriesFilter
    ) {
        // Precompute mapping from file hash to BibEntry
        Map<String, BibEntry> fileHashToEntry = new HashMap<>();
        for (FullBibEntry fullEntry : entriesFilter) {
            for (LinkedFile linkedFile : fullEntry.entry().getFiles()) {
                linkedFile.findIn(fullEntry.databaseContext(), filePreferences)
                          .flatMap(FileHasher::computeHash)
                          .ifPresent(hash -> fileHashToEntry.putIfAbsent(hash, fullEntry.entry()));
            }
        }
        List<String> fileHashes = new ArrayList<>(fileHashToEntry.keySet());

        Optional<Filter> filter = fileHashes.isEmpty()
                                  ? Optional.empty()
                                  : Optional.of(MetadataFilterBuilder
                .metadataKey(EmbeddingsCleaner.FILE_HASH_METADATA_KEY)
                .isIn(fileHashes));

        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest
                .builder()
                .maxResults(maximumResultsCount)
                .minScore(minimumScore)
                .filter(filter.orElse(null))
                .queryEmbedding(embeddingModel.embed(query).content())
                .build();

        EmbeddingSearchResult<TextSegment> embeddingSearchResult = embeddingStore.search(embeddingSearchRequest);

        List<RelevantInformation> excerpts = embeddingSearchResult
                .matches()
                .stream()
                .map(EmbeddingMatch::embedded)
                .map(textSegment -> {
                    String fileHash = textSegment.metadata().getString(EmbeddingsCleaner.FILE_HASH_METADATA_KEY);
                    String citationKey = Optional.ofNullable(fileHash != null ? fileHashToEntry.get(fileHash) : null)
                            .or(() -> findEntryByFileHash(entriesFilter, fileHash))
                            .flatMap(BibEntry::getCitationKey)
                            .orElse(null);
                    return new RelevantInformation(citationKey, textSegment.text());
                })
                .toList();

        LOGGER.debug("Found excerpts for the message: {}", excerpts);

        return excerpts;
    }

    /// Finds a BibEntry that has a LinkedFile with the given file hash.
    ///
    /// @param entries  the entries to search
    /// @param fileHash the SHA-256 hash of the file
    /// @return the entry if found
    Optional<BibEntry> findEntryByFileHash(List<FullBibEntry> entries, @Nullable String fileHash) {
        if (fileHash != null) {
            Optional<BibEntry> matched = entries
                    .stream()
                    .filter(fullEntry ->
                            fullEntry.entry()
                                     .getFiles()
                                     .stream()
                                     .anyMatch(linkedFile ->
                                             linkedFile.findIn(fullEntry.databaseContext(), filePreferences)
                                                       .flatMap(FileHasher::computeHash)
                                                       .filter(hash -> hash.equals(fileHash))
                                                       .isPresent()
                                     )
                    )
                    .map(FullBibEntry::entry)
                    .findFirst();

            if (matched.isPresent()) {
                return matched;
            }
        }

        if (entries.size() == 1) {
            return Optional.of(entries.getFirst().entry());
        }

        return Optional.empty();
    }

    @Override
    public AnswerEngineKind getKind() {
        return AnswerEngineKind.EMBEDDINGS_SEARCH;
    }
}
