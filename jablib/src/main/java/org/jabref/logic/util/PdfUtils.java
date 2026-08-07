package org.jabref.logic.util;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PdfUtils {

    /// Returns the contents of the first page ordered by their position on the page
    public static String getFirstPageContents(PDDocument document) throws IOException {
        // Sorting is required ÄÜP
        return getPageContents(document, 1, true);
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
