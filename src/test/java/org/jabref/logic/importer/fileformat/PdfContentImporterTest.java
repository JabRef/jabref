package org.jabref.logic.importer.fileformat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.FieldName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class PdfContentImporterTest {

    private PdfContentImporter importer;

    @BeforeEach
    public void setUp() {
        importer = new PdfContentImporter(mock(ImportFormatPreferences.class));
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(StandardFileType.PDF, importer.getFileType());
    }

    @Test
    public void testGetDescription() {
        assertEquals(
                     "PdfContentImporter parses data of the first page of the PDF and creates a BibTeX entry. Currently, Springer and IEEE formats are supported.",
                     importer.getDescription());
    }

    @Test
    public void doesNotHandleEncryptedPdfs() throws Exception {
        Path file = Paths.get(PdfContentImporter.class.getResource("/pdfs/encrypted.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void importTwiceWorksAsExpected() throws Exception {
        Path file = Paths.get(PdfContentImporter.class.getResource("/pdfs/minimal.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        BibEntry expected = new BibEntry(BibtexEntryTypes.INPROCEEDINGS);
        expected.setField(FieldName.AUTHOR, "1 ");
        expected.setField(FieldName.TITLE, "Hello World");

        List<BibEntry> resultSecondImport = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        assertEquals(Collections.singletonList(expected), result);
        assertEquals(Collections.singletonList(expected), resultSecondImport);

    }

}
