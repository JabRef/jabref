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
/// fetcher cost.
///
/// Two complementary maps:
///
/// 1. **Token cache** — keyed by a server-issued CUID2 returned from the
///    lookup endpoint. The matching add endpoint consumes the key. Short TTL
///    so a stale token can't resurrect an unrelated citation across sessions.
/// 2. **Text cache** — keyed by the raw citation text itself. Lets a repeat
///    `/lookup` (or batched `/lookup:batch`) skip the LLM / fetcher parse
///    entirely when the same citation string has been parsed recently (e.g.
///    SumatraPDF re-scans a visible PDF page after the user jumps back to it).
///    Longer TTL than the token cache because the parsed BibEntry stays valid
///    as long as the citation text does.
@Singleton
public class CitationCacheService {

    private static final int TOKEN_MAX_ENTRIES = 256;
    private static final Duration TOKEN_TTL = Duration.ofHours(1);
    private static final int TEXT_MAX_ENTRIES = 1024;
    private static final Duration TEXT_TTL = Duration.ofHours(24);
    /// Token length picked to match the CUID2 size already used by
    /// [BibFieldsIndexer]: long enough to be collision-resistant in a 256-slot
    /// cache without being noisy in HTTP responses.
    private static final int CUID_LENGTH = 12;

    public record CachedCitation(BibEntry parsed, String rawCitation, Instant createdAt) {
    }

    private final Cache<String, CachedCitation> tokenCache = Caffeine.newBuilder()
                                                                     .maximumSize(TOKEN_MAX_ENTRIES)
                                                                     .expireAfterWrite(TOKEN_TTL)
                                                                     .build();

    private final Cache<String, CachedCitation> textCache = Caffeine.newBuilder()
                                                                    .maximumSize(TEXT_MAX_ENTRIES)
                                                                    .expireAfterWrite(TEXT_TTL)
                                                                    .build();

    /// Stores a parsed citation and returns the token the client should send
    /// back to the add endpoint.
    public String put(BibEntry parsed, String rawCitation) {
        String key = CUID.randomCUID2(CUID_LENGTH).toString();
        tokenCache.put(key, new CachedCitation(parsed, rawCitation, Instant.now()));
        return key;
    }

    /// Returns the cached citation, or empty when the token has expired or
    /// was never issued. Does **not** evict on read so the same lookup result
    /// can be added more than once (idempotent across retries).
    public Optional<CachedCitation> get(String key) {
        return Optional.ofNullable(tokenCache.getIfPresent(key));
    }

    /// Drops the cache entry; called by the add endpoint after a successful
    /// import so a stale token can't be re-used to add a duplicate.
    public void invalidate(String key) {
        tokenCache.invalidate(key);
    }

    /// Stashes a parsed citation under its raw text so a later `/lookup` for
    /// the same text can skip the LLM parse. The token-keyed cache is left
    /// untouched — callers mint a fresh token via [#put] for each lookup.
    public void putByText(BibEntry parsed, String rawCitation) {
        textCache.put(rawCitation, new CachedCitation(parsed, rawCitation, Instant.now()));
    }

    /// Returns the cached parse for the given citation text, or empty on miss.
    public Optional<CachedCitation> getByText(String citationText) {
        return Optional.ofNullable(textCache.getIfPresent(citationText));
    }
}
