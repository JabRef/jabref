package org.jabref.logic.importer.relatedwork;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.jabref.logic.importer.RelatedWorkAnnotator;
import org.jabref.model.entry.BibEntry;

public class RelatedWorkHarvester {

    private final RelatedWorkExtractor extractor;

    public RelatedWorkHarvester(RelatedWorkExtractor extractor) {
        this.extractor = extractor;
    }

    /**
     * Existing API
     */
    public void harvestAndAnnotate(
            String username,
            String citingPaperKey,
            String fullText,
            List<BibEntry> bibliography,
            Consumer<BibEntry> addOrUpdateFn
    ) {
        annotateInternal(username, citingPaperKey, fullText, bibliography, addOrUpdateFn);
    }

    /**
     * Test-friendly: returns number of entries annotated.
     */
    public int harvestAndAnnotateCount(
            String username,
            String citingPaperKey,
            String fullText,
            List<BibEntry> bibliography,
            Consumer<BibEntry> addOrUpdateFn
    ) {
        return annotateInternal(username, citingPaperKey, fullText, bibliography, addOrUpdateFn);
    }

    private int annotateInternal(
            String username,
            String citingPaperKey,
            String fullText,
            List<BibEntry> bibliography,
            Consumer<BibEntry> addOrUpdateFn
    ) {
        Map<String, String> summaries = extractor.extract(fullText, bibliography);
        int updated = 0;

        for (Map.Entry<String, String> e : summaries.entrySet()) {
            String citedKey = e.getKey();
            String summary = e.getValue();

            BibEntry entry = findOrCreateEntry(citedKey, bibliography, addOrUpdateFn);
            RelatedWorkAnnotator.appendSummaryToEntry(entry, username, citingPaperKey, summary);
            addOrUpdateFn.accept(entry);
            updated++;
        }
        return updated;
    }

    private BibEntry findOrCreateEntry(String key, List<BibEntry> bibs, Consumer<BibEntry> addOrUpdateFn) {
        Optional<BibEntry> found = bibs.stream()
                                       .filter(b -> b.getCitationKey().orElse("").equals(key))
                                       .findFirst();

        if (found.isPresent()) {
            return found.get();
        }

        BibEntry newEntry = new BibEntry();
        newEntry.setCitationKey(key);
        addOrUpdateFn.accept(newEntry);
        return newEntry;
    }
}
