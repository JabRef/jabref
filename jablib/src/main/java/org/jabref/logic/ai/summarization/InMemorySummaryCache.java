package org.jabref.logic.ai.summarization;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.entry.BibEntry;

/// Session-scoped RAM cache for AI summaries.
///
/// Keyed by {@link BibEntry} *reference identity* (using {@link IdentityHashMap}), so it
/// works even for entries that have no citation key or a non-unique one.
///
/// On {@link #close()}, entries whose {@link BibEntry} has a *present and unique*
/// citation key and a valid AI library ID are flushed to the persistent {@link SummariesRepository}.
///
/// Thread-safe: all map operations are protected by a synchronized wrapper.
public class InMemorySummaryCache {

    private record CachedEntry(AiSummary summary, FullBibEntry fullEntry) {
    }

    // IdentityHashMap: compares keys by reference (==), NOT by equals()/hashCode().
    // This is exactly what we need — two BibEntry objects with the same citation key are distinct
    // cache slots. No citation key is required at all.
    private final Map<BibEntry, CachedEntry> cache = Collections.synchronizedMap(new IdentityHashMap<>());

    private final SummariesRepository repository;

    public InMemorySummaryCache(SummariesRepository repository) {
        this.repository = repository;
    }

    /// Stores `summary` for `fullEntry`. Overwrites any previously cached summary for the
    /// same object reference.
    public void put(FullBibEntry fullEntry, AiSummary summary) {
        cache.put(fullEntry.entry(), new CachedEntry(summary, fullEntry));
    }

    /// Returns the cached summary for `entry`, or {@link Optional#empty()} if none exists.
    public Optional<AiSummary> get(BibEntry entry) {
        return Optional.ofNullable(cache.get(entry)).map(CachedEntry::summary);
    }

    /// Removes the cached summary for `entry`. No-op if none was present.
    public void remove(BibEntry entry) {
        cache.remove(entry);
    }

    /// Writes all cached summaries to the persistent repository for entries whose citation key is
    /// *present and unique* and whose database has a valid AI library ID.
    /// Entries that do not satisfy both conditions are silently skipped.
    ///
    /// Call this when the library or application is closing so that valid summaries survive
    /// the next restart.
    public void close() {
        cache.forEach((bibEntry, cached) -> {
            // If the BibEntry was deleted from the database, the summary must not be saved.
            if (!cached.fullEntry.databaseContext().getDatabase().getEntries().contains(bibEntry)) {
                return;
            }

            cached.fullEntry().toAiSummaryIdentifier()
                  .ifPresent(id -> repository.set(id, cached.summary()));
        });
    }
}
