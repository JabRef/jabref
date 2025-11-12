package org.jabref.logic.importer.relatedwork;

import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

/**
 * Optional summarizer that can condense one or more raw snippets for a cited entry
 * into a single, shorter summary. Implementations may call external services.
 * <p>
 * This SPI is AI-neutral: the default impl is a no-op.
 */
public interface RelatedWorkSummarizer {

    /**
     * @param snippets raw snippets captured around in-text citations for the same BibEntry (non-empty)
     * @param entry the cited entry to be summarized (context)
     * @param maxLen a soft maximum length for the summary (implementations may exceed)
     * @return an Optional summary. Empty means "do not summarize; use original snippets".
     */
    Optional<String> summarize(List<String> snippets, BibEntry entry, int maxLen);
}
