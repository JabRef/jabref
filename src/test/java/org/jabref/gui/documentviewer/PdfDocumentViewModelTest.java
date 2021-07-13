package org.jabref.gui.documentviewer;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfDocumentViewModelTest {

  @Test
  void getPagesTest(@TempDir Path tempDir) throws IOException {
    try (PDDocument mockPDF = new PDDocument()) {
      Path pdfFile = tempDir.resolve("mockPDF.pdf");

      mockPDF.addPage(new PDPage());
      mockPDF.save(pdfFile.toAbsolutePath().toString());

      PdfDocumentViewModel PDFviewModel = new PdfDocumentViewModel(mockPDF);

      assertEquals(1, PDFviewModel.getPages().size());
    }
  }
}
