package org.jabref.http.server.services;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.thibaultmeyer.cuid.CUID;
import jakarta.inject.Singleton;

/// Caches the result of a plain-citation → BibEntry conversion so a hover
/// existence check and a subsequent add-to-library don't both pay the LLM /
/// fetcher cost. Cache keys are server-issued CUID2 strings returned from the
/// lookup endpoint; the matching add endpoint consumes the key.
///
/// Bounded to keep memory tight (256 entries, ~5 KB each ≈ 1.25 MB worst case)
/// and time-bounded to 1 hour so a stale token left over from a previous
/// session can't resurrect an unrelated citation.
@Singleton
public class CitationCacheService {

    private static final int MAX_ENTRIES = 256;
    private static final Duration TTL = Duration.ofHours(1);
    /// Token length picked to match the CUID2 size already used by
    /// [BibFieldsIndexer]: long enough to be collision-resistant in a 256-slot
    /// cache without being noisy in HTTP responses.
    private static final int CUID_LENGTH = 12;

    public record CachedCitation(BibEntry parsed, String rawCitation, Instant createdAt) {
    }

    private final Cache<String, CachedCitation> cache = Caffeine.newBuilder()
                                                                .maximumSize(MAX_ENTRIES)
                                                                .expireAfterWrite(TTL)
                                                                .build();

    /// Stores a parsed citation and returns the token the client should send
    /// back to the add endpoint.
    public String put(BibEntry parsed, String rawCitation) {
        String key = CUID.randomCUID2(CUID_LENGTH).toString();
        cache.put(key, new CachedCitation(parsed, rawCitation, Instant.now()));
        return key;
    }

    /// Returns the cached citation, or empty when the token has expired or
    /// was never issued. Does **not** evict on read so the same lookup result
    /// can be added more than once (idempotent across retries).
    public Optional<CachedCitation> get(String key) {
        return Optional.ofNullable(cache.getIfPresent(key));
    }

    /// Drops the cache entry; called by the add endpoint after a successful
    /// import so a stale token can't be re-used to add a duplicate.
    public void invalidate(String key) {
        cache.invalidate(key);
    }
}
