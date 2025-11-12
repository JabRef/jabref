package org.jabref.logic.importer.relatedwork;

import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

/**
 * Optional resolver that can match unresolved citations to an existing BibEntry
 * or create a new one. Implementations may use heuristics or external services.
 * <p>
 * This SPI is AI-neutral: the default impl is a no-op.
 */
public interface CitationResolver {

    /**
     * Resolve a citation by its citation key string (e.g., "Vesce2016").
     *
     * @param citationKey the citation key produced by the extractor
     * @param candidates existing candidate entries to search through
     * @return a matching BibEntry, or empty if none
     */
    Optional<BibEntry> resolveByKey(String citationKey, List<BibEntry> candidates);

    /**
     * Resolve a citation by (surname, year) when available.
     *
     * @param surnameLower lowercase surname token (e.g., "vesce")
     * @param year four-digit year (e.g., "2016")
     * @param candidates existing candidate entries to search through
     * @return a matching BibEntry, or empty if none
     */
    Optional<BibEntry> resolveByAuthorYear(String surnameLower, String year, List<BibEntry> candidates);

    /**
     * Optionally create a new entry when no match is found.
     * Default no-op returns empty; implementations may synthesize a shell entry.
     *
     * @param citationDisplay a human-friendly label to initialize title/note if needed
     * @return a newly created BibEntry, or empty
     */
    Optional<BibEntry> createIfMissing(String citationDisplay);
}
