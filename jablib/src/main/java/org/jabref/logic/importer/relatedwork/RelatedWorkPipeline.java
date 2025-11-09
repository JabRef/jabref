package org.jabref.logic.importer.relatedwork;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

/**
 * Wires the section locator + extractor:
 * - finds the Related Work body in the full text
 * - extracts per-citation snippets from that section
 */
public final class RelatedWorkPipeline {

    private final RelatedWorkSectionLocator locator;
    private final HeuristicRelatedWorkExtractor extractor;

    public RelatedWorkPipeline(HeuristicRelatedWorkExtractor extractor) {
        this.locator = new RelatedWorkSectionLocator();
        this.extractor = extractor;
    }

    /**
     * Full end-to-end step: locate section, then extract snippets.
     *
     * @param fullText entire plain-text of the paper
     * @param candidateEntries entries we might cite
     * @return map: citationKey -> extracted snippet
     */
    public Map<String, String> run(String fullText, List<BibEntry> candidateEntries) {
        if (fullText == null || fullText.isEmpty()) {
            return Collections.emptyMap();
        }

        // Use the static helper to find the section span
        Optional<RelatedWorkSectionLocator.SectionSpan> opt =
                RelatedWorkSectionLocator.locateStatic(fullText);

        if (opt.isEmpty()) {
            return Collections.emptyMap();
        }

        RelatedWorkSectionLocator.SectionSpan span = opt.get();
        String sectionText = fullText.substring(span.startOffset, span.endOffset);

        // The extractor expects a String for the section body
        return extractor.extract(sectionText, candidateEntries);
    }
}
