package org.jabref.gui.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDPageLabels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfPageLabelResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfPageLabelResolver.class);

    private PdfPageLabelResolver() {
    }

    /// Resolves a logical page number (from PDF page labels) to a physical page number (1-based).
    /// Falls back to the original logical page number when no label mapping exists.
    public static int resolvePhysicalPageNumber(Path pdfPath, int logicalPageNumber) {
        int normalizedLogicalPageNumber = Math.max(logicalPageNumber, 1);

        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            int totalPages = document.getNumberOfPages();
            PDPageLabels pageLabels = document.getDocumentCatalog().getPageLabels();
            if (pageLabels == null) {
                return clampPageNumber(normalizedLogicalPageNumber, totalPages);
            }

            Map<String, Integer> pageIndexByLabel = pageLabels.getPageIndicesByLabels();
            Integer pageIndex = pageIndexByLabel.get(String.valueOf(normalizedLogicalPageNumber));
            if (pageIndex == null) {
                return clampPageNumber(normalizedLogicalPageNumber, totalPages);
            }

            return clampPageNumber(pageIndex + 1, totalPages);
        } catch (IOException exception) {
            LOGGER.debug("Could not resolve page label mapping for {}", pdfPath, exception);
            return normalizedLogicalPageNumber;
        }
    }

    private static int clampPageNumber(int pageNumber, int totalPages) {
        if (totalPages <= 0) {
            return 1;
        }

        if (pageNumber < 1) {
            return 1;
        }

        if (pageNumber > totalPages) {
            return totalPages;
        }

        return pageNumber;
    }
}
