package org.jabref.logic.pdf;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDPageLabelRange;
import org.apache.pdfbox.pdmodel.common.PDPageLabels;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfPageLabelResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesLogicalPageToPhysicalPageUsingPageLabels() throws IOException {
        Path pdfFile = tempDir.resolve("labeled.pdf");
        createPdfWithFrontMatterAndDecimalLabels(pdfFile);

        assertEquals(3, PdfPageLabelResolver.resolvePhysicalPageNumber(pdfFile, 1));
        assertEquals(5, PdfPageLabelResolver.resolvePhysicalPageNumber(pdfFile, 3));
    }

    @Test
    void fallsBackToGivenPageWhenLabelsAreMissing() throws IOException {
        Path pdfFile = tempDir.resolve("plain.pdf");
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < 4; i++) {
                document.addPage(new PDPage());
            }
            document.save(pdfFile.toFile());
        }

        assertEquals(3, PdfPageLabelResolver.resolvePhysicalPageNumber(pdfFile, 3));
    }

    private static void createPdfWithFrontMatterAndDecimalLabels(Path pdfFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < 5; i++) {
                document.addPage(new PDPage());
            }

            PDPageLabels labels = new PDPageLabels(document);

            PDPageLabelRange frontMatter = new PDPageLabelRange();
            frontMatter.setStyle(PDPageLabelRange.STYLE_ROMAN_LOWER);
            frontMatter.setStart(1);
            labels.setLabelItem(0, frontMatter);

            PDPageLabelRange mainContent = new PDPageLabelRange();
            mainContent.setStyle(PDPageLabelRange.STYLE_DECIMAL);
            mainContent.setStart(1);
            labels.setLabelItem(2, mainContent);

            document.getDocumentCatalog().setPageLabels(labels);
            document.save(pdfFile.toFile());
        }
    }
}
