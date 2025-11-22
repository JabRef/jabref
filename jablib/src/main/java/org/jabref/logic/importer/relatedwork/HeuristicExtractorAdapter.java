package org.jabref.logic.importer.relatedwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.model.entry.BibEntry;

/**
 * Adapts HeuristicRelatedWorkExtractor (citationKey -> snippet) to the
 * RelatedWorkEvaluationRunner.Extractor interface (BibEntry -> snippets).
 */
public final class HeuristicExtractorAdapter implements RelatedWorkEvaluationRunner.Extractor {

    private final HeuristicRelatedWorkExtractor delegate;

    public HeuristicExtractorAdapter(HeuristicRelatedWorkExtractor delegate) {
        this.delegate = delegate;
    }

    @Override
    public Map<BibEntry, List<String>> apply(String relatedWorkText, List<BibEntry> candidates) {
        Map<String, String> byKey = delegate.extract(relatedWorkText, candidates);

        Map<String, BibEntry> entryByKey = new HashMap<>();
        for (BibEntry be : candidates) {
            be.getCitationKey().ifPresent(k -> entryByKey.put(k, be));
        }

        Map<BibEntry, List<String>> out = new HashMap<>();
        for (Map.Entry<String, String> e : byKey.entrySet()) {
            BibEntry be = entryByKey.get(e.getKey());
            if (be == null) {
                continue; // no match for that citation key among candidates
            }
            out.computeIfAbsent(be, k -> new ArrayList<>()).add(e.getValue());
        }
        return out;
    }
}
