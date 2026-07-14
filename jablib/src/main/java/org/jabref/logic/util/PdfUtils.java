package org.jabref.logic.util;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.entry.identifier.DOI;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PdfUtils {

    /// Returns the contents of the first page ordered by their position on the page
    public static String getFirstPageContents(PDDocument document) throws IOException {
        // Sorting is required ÄÜP
        return getPageContents(document, 1, true);
    }

    /// Searches the PDF file for the first DOI it contains.
    ///
    /// The first page is searched first, as this is the most common location for a DOI; if none is
    /// found there, the remaining pages are scanned in order.
    ///
    /// @param filePath the PDF file to scan
    /// @return the first {@link DOI} found in the PDF, or an empty Optional if none is present
    public static Optional<DOI> getFirstDoi(Path filePath) throws IOException {
        try (PDDocument document = new XmpUtilReader().loadWithAutomaticDecryption(filePath)) {
            return getFirstDoi(document);
        }
    }

    /// Searches the PDF document for the first DOI it contains.
    ///
    /// @see #getFirstDoi(Path)
    public static Optional<DOI> getFirstDoi(PDDocument document) throws IOException {
        int numberOfPages = document.getNumberOfPages();
        for (int page = 1; page <= numberOfPages; page++) {
            Optional<DOI> doi = DOI.findInText(getPageContents(document, page));
            if (doi.isPresent()) {
                return doi;
            }
        }
        return Optional.empty();
    }

    public static String getPageContents(PDDocument document, int page) throws IOException {
        return getPageContents(document, page, false);
    }

    /// @param setSortByPosition - "true" might have issues with IEEE
    public static String getPageContents(PDDocument document, int page, boolean setSortByPosition) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        stripper.setSortByPosition(setSortByPosition);
        // stripper.setParagraphEnd(System.lineSeparator()); // not sure about the side effects
        StringWriter writer = new StringWriter();
        stripper.writeText(document, writer);
        return writer.toString();
    }
}
