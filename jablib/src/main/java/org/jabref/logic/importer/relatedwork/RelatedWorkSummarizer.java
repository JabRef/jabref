package org.jabref.logic.importer.relatedwork;

import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

/**
 * SPI for optional AI-assisted summarization of related-work snippets.
 * <p>
 * Implementations receive:
 * - a list of sentence-like snippets that all describe the same cited paper
 * - the target BibEntry (for context: key, title, etc.)
 * - a soft maximum length for the resulting summary (in characters)
 * <p>
 * They may return:
 * - Optional.of(summary) if a useful, concise summary can be produced
 * - Optional.empty() to signal "no summary / fall back to raw snippets"
 */
@FunctionalInterface
public interface RelatedWorkSummarizer {

    /**
     * Produce a concise description of a cited paper based on related-work snippets.
     *
     * @param snippets sentence-like fragments taken from the citing paper's related-work section
     * @param entry the BibEntry corresponding to the cited paper
     * @param maxLen soft limit on the resulting description length (in characters); implementations are free
     * to interpret this as a guideline rather than a strict cutoff
     * @return optional summary text
     */
    Optional<String> summarize(List<String> snippets, BibEntry entry, int maxLen);
}
