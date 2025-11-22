package org.jabref.logic.importer.relatedwork;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jabref.logic.importer.RelatedWorkAnnotator;
import org.jabref.model.entry.BibEntry;

/**
 * End-to-end helper for callers that already have candidate entries:
 * PDF -> section -> extractor -> annotator.
 */
public final class RelatedWorkPdfPipeline {

    private final PdfRelatedWorkTextExtractor pdfSectionExtractor;
    private final HeuristicRelatedWorkExtractor extractor;
    private final RelatedWorkAnnotator annotator;

    public RelatedWorkPdfPipeline(PdfRelatedWorkTextExtractor pdfSectionExtractor,
                                  HeuristicRelatedWorkExtractor extractor,
                                  RelatedWorkAnnotator annotator) {
        this.pdfSectionExtractor = Objects.requireNonNull(pdfSectionExtractor);
        this.extractor = Objects.requireNonNull(extractor);
        this.annotator = Objects.requireNonNull(annotator);
    }

    /**
     * @return number of annotations appended across all matched entries
     */
    public int run(Path citingPdf,
                   List<BibEntry> candidateEntries,
                   String citingKey,
                   String username) throws IOException {

        return pdfSectionExtractor.extractRelatedWorkSection(citingPdf)
                                  .map(section -> {
                                      // 1) Extract citationKey -> snippet
                                      Map<String, String> snippets = extractor.extract(section, candidateEntries);

                                      // 2) Index candidates by citation key
                                      Map<String, BibEntry> byKey = new HashMap<>();
                                      for (BibEntry be : candidateEntries) {
                                          be.getCitationKey().ifPresent(k -> byKey.put(k, be));
                                      }

                                      // 3) Append to matching entries
                                      int appended = 0;
                                      for (Map.Entry<String, String> e : snippets.entrySet()) {
                                          BibEntry target = byKey.get(e.getKey());
                                          if (target != null) {
                                              // Adjust arg order to match your actual method signature if needed:
                                              // appendSummaryToEntry(target, citingKey, summary, username)
                                              annotator.appendSummaryToEntry(target, citingKey, e.getValue(), username);
                                              appended++;
                                          }
                                      }
                                      return appended;
                                  })
                                  .orElse(0);
    }
}
