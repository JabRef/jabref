package org.jabref.logic.importer.relatedwork;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Adapter that: PDF -> full plain text (via PdfTextProvider) -> "Related Work" block (via RelatedWorkSectionLocator).
 * No PDF dependencies here; all PDF specifics live behind PdfTextProvider.
 */
public final class PdfRelatedWorkTextExtractor {

    private final PdfTextProvider pdfTextProvider;
    private final RelatedWorkSectionLocator sectionLocator;

    public PdfRelatedWorkTextExtractor(PdfTextProvider pdfTextProvider,
                                       RelatedWorkSectionLocator sectionLocator) {
        this.pdfTextProvider = Objects.requireNonNull(pdfTextProvider);
        this.sectionLocator = Objects.requireNonNull(sectionLocator);
    }

    /**
     * Extracts the "Related Work"/"Literature Review" section from the given PDF, if present.
     *
     * @param pdf path to the PDF file
     * @return Optional with the related-work block (no header), or empty if not found / empty text
     * @throws IOException if reading the PDF fails
     * @throws IllegalArgumentException if the path is invalid
     */
    public Optional<String> extractRelatedWorkSection(Path pdf) throws IOException {
        Objects.requireNonNull(pdf, "pdf");
        if (!Files.isRegularFile(pdf)) {
            throw new IllegalArgumentException("Not a regular file: " + pdf);
        }

        Optional<String> plain = pdfTextProvider.extractPlainText(pdf);
        if (plain.isEmpty() || plain.get().isBlank()) {
            return Optional.empty();
        }

        String text = plain.get();
        return sectionLocator.locate(text)
                             .map(span -> {
                                 int start = Math.max(0, span.startOffset);  // body start
                                 int end = Math.min(text.length(), span.endOffset); // body end
                                 if (start >= end) {
                                     return "";
                                 }
                                 // Header already excluded by startOffset, so no need to strip it again.
                                 return text.substring(start, end).trim();
                             })
                             .filter(s -> !s.isBlank());
    }
}
