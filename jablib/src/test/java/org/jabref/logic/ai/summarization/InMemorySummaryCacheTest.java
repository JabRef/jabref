package org.jabref.logic.ai.summarization;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.ai.summarization.AiSummaryIdentifier;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemorySummaryCacheTest {

    private static final String LIBRARY_ID = "test-lib-id";

    private FakeSummariesRepository fakeRepository;
    private InMemorySummaryCache cache;

    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() {
        fakeRepository = new FakeSummariesRepository();
        cache = new InMemorySummaryCache(fakeRepository);

        MetaData metaData = new MetaData();
        metaData.setAiLibraryId(LIBRARY_ID);
        BibDatabase database = new BibDatabase();
        databaseContext = new BibDatabaseContext(database, metaData);
    }

    private static AiSummary buildSummary(String content) {
        AiMetadata metadata = new AiMetadata(AiProvider.OPEN_AI, "gpt-4", Instant.now());
        return new AiSummary(metadata, SummarizatorKind.CHUNKED, content);
    }

    @Test
    void putAndGetReturnsSameSummary() {
        BibEntry entry = new BibEntry().withCitationKey("Smith2024");
        databaseContext.getDatabase().insertEntry(entry);
        FullBibEntry fullEntry = new FullBibEntry(databaseContext, entry);

        AiSummary summary = buildSummary("great paper");
        cache.put(fullEntry, summary);

        Optional<AiSummary> result = cache.get(entry);
        assertTrue(result.isPresent());
        assertEquals("great paper", result.get().content());
    }

    @Test
    void getReturnsEmptyWhenNothingCached() {
        BibEntry entry = new BibEntry().withCitationKey("Unknown2024");

        Optional<AiSummary> result = cache.get(entry);

        assertFalse(result.isPresent());
    }

    @Test
    void removeErasesTheCachedSummary() {
        BibEntry entry = new BibEntry().withCitationKey("Jones2024");
        databaseContext.getDatabase().insertEntry(entry);
        FullBibEntry fullEntry = new FullBibEntry(databaseContext, entry);

        cache.put(fullEntry, buildSummary("to be removed"));
        cache.remove(entry);

        assertFalse(cache.get(entry).isPresent());
    }

    @Test
    void closeFlushesSummariesToRepository() {
        BibEntry entry = new BibEntry().withCitationKey("Flush2024");
        databaseContext.getDatabase().insertEntry(entry);
        FullBibEntry fullEntry = new FullBibEntry(databaseContext, entry);

        cache.put(fullEntry, buildSummary("important content"));
        cache.close();

        AiSummaryIdentifier id = new AiSummaryIdentifier(LIBRARY_ID, "Flush2024");
        Optional<AiSummary> persisted = fakeRepository.get(id);
        assertTrue(persisted.isPresent());
        assertEquals("important content", persisted.get().content());
    }

    @Test
    void closeSkipsEntriesRemovedFromDatabase() {
        BibEntry entry = new BibEntry().withCitationKey("Deleted2024");
        databaseContext.getDatabase().insertEntry(entry);
        FullBibEntry fullEntry = new FullBibEntry(databaseContext, entry);

        cache.put(fullEntry, buildSummary("should not be persisted"));
        databaseContext.getDatabase().removeEntry(entry);

        cache.close();

        AiSummaryIdentifier id = new AiSummaryIdentifier(LIBRARY_ID, "Deleted2024");
        assertFalse(fakeRepository.get(id).isPresent());
    }

    @Test
    void putOverwritesPreviousSummary() {
        BibEntry entry = new BibEntry().withCitationKey("Over2024");
        databaseContext.getDatabase().insertEntry(entry);
        FullBibEntry fullEntry = new FullBibEntry(databaseContext, entry);

        cache.put(fullEntry, buildSummary("original"));
        cache.put(fullEntry, buildSummary("updated"));

        assertEquals("updated", cache.get(entry).get().content());
    }

    /// Minimal in-memory implementation of {@link SummariesRepository} for testing.
    private static class FakeSummariesRepository implements SummariesRepository {

        private final Map<String, AiSummary> store = new HashMap<>();

        private String key(AiSummaryIdentifier id) {
            return id.libraryId() + "/" + id.summaryName();
        }

        @Override
        public void set(AiSummaryIdentifier summaryIdentifier, AiSummary aiSummary) {
            store.put(key(summaryIdentifier), aiSummary);
        }

        @Override
        public Optional<AiSummary> get(AiSummaryIdentifier summaryIdentifier) {
            return Optional.ofNullable(store.get(key(summaryIdentifier)));
        }

        @Override
        public void clear(AiSummaryIdentifier summaryIdentifier) {
            store.remove(key(summaryIdentifier));
        }
    }
}
