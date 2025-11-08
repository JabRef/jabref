package org.jabref.logic.importer.relatedwork;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jabref.logic.importer.RelatedWorkAnnotator;
import org.jabref.model.entry.BibEntry;

/**
 * Given full plain text of a citing paper and a list/database of candidate entries,
 * extract contextual snippets for each cited entry and append them via RelatedWorkAnnotator.
 */
public final class RelatedWorkPipeline {

    private final RelatedWorkSectionLocator locator;
    private final HeuristicRelatedWorkExtractor extractor;
    private final RelatedWorkAnnotator annotator;

    public RelatedWorkPipeline(RelatedWorkSectionLocator locator,
                               HeuristicRelatedWorkExtractor extractor,
                               RelatedWorkAnnotator annotator) {
        this.locator = Objects.requireNonNull(locator);
        this.extractor = Objects.requireNonNull(extractor);
        this.annotator = Objects.requireNonNull(annotator);
    }

    /**
     * @param fullPlainText    full plaintext of the citing paper
     * @param candidateEntries entries that may be cited (must have citation keys)
     * @param citingKey        citation key of the citing paper (e.g., Smith2021)
     * @param username         username to select the {@code comment-&lt;username&gt;} field
     * @return count of cited entries we attempted to annotate (i.e., had a target & snippet)
     */
    public int run(String fullPlainText,
                   List<BibEntry> candidateEntries,
                   String citingKey,
                   String username) {

        return locator.locate(fullPlainText).map(sectionText -> {
            // key -> entry map for quick lookup
            Map<String, BibEntry> byKey = new HashMap<>();
            for (BibEntry e : candidateEntries) {
                e.getCitationKey().ifPresent(key -> byKey.put(key, e));
            }

            // Extract snippets keyed by cited entry key
            Map<String, String> snippetsByCitedKey = extractor.extract(sectionText, candidateEntries);

            int attempts = 0;
            for (Map.Entry<String, String> hit : snippetsByCitedKey.entrySet()) {
                String citedKey = hit.getKey();
                String snippet = hit.getValue();

                BibEntry target = byKey.get(citedKey);
                if (target == null || snippet == null || snippet.isBlank()) {
                    continue; // nothing to do
                }

                annotator.appendSummaryToEntry(target, citingKey, snippet, username);
                attempts++; // we successfully invoked the annotator for this target
            }
            return attempts;
        }).orElse(0);
    }
}
