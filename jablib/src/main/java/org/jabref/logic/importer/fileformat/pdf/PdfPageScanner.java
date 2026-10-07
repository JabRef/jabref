package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.util.PdfUtils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class PdfPageScanner {

    private static final double PAGE_SCAN_PERCENTAGE = 0.05;
    private static final int MIN_PAGES_PER_END = 3; //even if 5% is only 1 scan at least 3 
    private static final int MAX_PAGES_PER_END = 10; //scan at most 10 

    private PdfPageScanner() {
    }

    /// Extracts text from candidate pages at the beginning and end of a PDF.
    static List<String> scanPages(PDDocument document) throws IOException {
        int totalPages = document.getNumberOfPages();

        int pagesPerEnd = (int) Math.ceil(totalPages * PAGE_SCAN_PERCENTAGE);
        pagesPerEnd = Math.max(MIN_PAGES_PER_END, pagesPerEnd);
        pagesPerEnd = Math.min(MAX_PAGES_PER_END, pagesPerEnd);
        pagesPerEnd = Math.min(pagesPerEnd, totalPages);

        int frontEnd = pagesPerEnd;
        int backStart = Math.max(frontEnd + 1, totalPages - pagesPerEnd + 1);

        List<String> pageTexts = new ArrayList<>();

        for (int page = 1; page <= frontEnd; page++) {
            pageTexts.add(PdfUtils.getPageContents(document, page));
        }

        for (int page = backStart; page <= totalPages; page++) {
            pageTexts.add(PdfUtils.getPageContents(document, page));
        }

        return pageTexts;
    }
}