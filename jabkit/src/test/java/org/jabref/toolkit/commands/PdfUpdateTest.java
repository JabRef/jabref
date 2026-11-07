package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        XmpPreferences xmpPreferences = new XmpPreferences(false, Set.of(), new SimpleObjectProperty<>(','));
        when(preferences.getXmpPreferences()).thenReturn(xmpPreferences);
        when(preferences.getFilePreferences().shouldStoreFilesRelativeToBibFile()).thenReturn(true);

        int exitCode = commandLine.executeToLog(
                "pdf", "update",
                "--input=" + bibFile,
                "--input-format=bibtex",
                "--format=xmp",
                "--citation-key=TestEntry");

        assertEquals(0, exitCode);

        List<BibEntry> writtenEntries = new XmpUtilReader().readXmp(pdfFile, xmpPreferences);
        assertEquals(1, writtenEntries.size(), "No XMP metadata found in PDF after update");
        assertEquals("Test Author", writtenEntries.getFirst().getField(StandardField.AUTHOR).orElse(""),
                "Author field in XMP metadata does not match the bib entry");
    }
}
