package org.jabref.logic.importer.relatedwork;

import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

/**
 * Default no-op: never summarizes; pipeline will keep raw snippets.
 */
public final class NoOpRelatedWorkSummarizer implements RelatedWorkSummarizer {

    @Override
    public Optional<String> summarize(List<String> snippets, BibEntry entry, int maxLen) {
        return Optional.empty();
    }
}
