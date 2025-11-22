package org.jabref.logic.importer.relatedwork;

import java.util.List;
import java.util.Map;

import org.jabref.model.entry.BibEntry;

/**
 * Interface for components that extract citation summaries from a paper's text.
 */
public interface RelatedWorkExtractor {
    /**
     * Extracts a mapping from cited paper keys to short summary sentences from the "Related Work" section.
     *
     * @param fullText full text of the citing paper
     * @param bibliography list of BibEntries referenced by the citing paper
     * @return map from citation key (of cited paper) to its descriptive summary text
     */
    Map<String, String> extract(String fullText, List<BibEntry> bibliography);
}
