package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.xmp.XmpPreferences;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

class PdfUpdateTest extends AbstractJabKitTest {

    @Test
    void xmpMetadataWrittenToLinkedPdfByCitationKey(@TempDir Path tempDir) throws IOException {
        Path pdfFile = tempDir.resolve("test.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pdfFile.toFile());
        }

        Path bibFile = tempDir.resolve("test.bib");
        Files.writeString(bibFile, """
                @Article{TestEntry,
                  author = {Test Author},
                  title  = {Test Title},
                  year   = {2023},
                  file   = {:test.pdf:PDF},
                }
                """);

        when(preferences.getXmpPreferences()).thenReturn(new XmpPreferences(false, Set.of(), new SimpleObjectProperty<>(',')));
        when(preferences.getFilePreferences().shouldStoreFilesRelativeToBibFile()).thenReturn(true);

        byte[] contentBefore = Files.readAllBytes(pdfFile);

        int exitCode = commandLine.executeToLog(
                "pdf", "update",
                "--input=" + bibFile,
                "--input-format=bibtex",
                "--format=xmp",
                "--citation-key=TestEntry");

        assertEquals(0, exitCode);
        assertFalse(Arrays.equals(contentBefore, Files.readAllBytes(pdfFile)),
                "PDF file should have been modified with XMP metadata, but content did not change");
    }
}
