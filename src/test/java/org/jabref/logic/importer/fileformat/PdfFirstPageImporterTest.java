package org.jabref.logic.importer.fileformat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PdfFirstPageImporterTest {

    private PdfFirstPageImporter importer;
    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getFieldContentFormatterPreferences().getNonWrappableFields()).thenReturn(List.of());
        importer = new PdfFirstPageImporter(importFormatPreferences);
    }

    @Test
    void testsGetExtensions() {
        assertEquals(StandardFileType.PDF, importer.getFileType());
    }

    @Test
    void testGetDescription() {
        assertEquals("PdfFirstPageImporter imports a verbatim BibTeX entry from the first page of the PDF.",
                     importer.getDescription());
    }

    @Test
    void doesNotHandleEncryptedPdfs() throws Exception {
        Path file = Path.of(PdfFirstPageImporter.class.getResource("/pdfs/encrypted.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void importTwiceWorksAsExpected() throws Exception {
        Path file = Path.of(PdfFirstPageImporterTest.class.getResource("mixedMetadata.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        BibEntry expected = new BibEntry(StandardEntryType.Article);
        expected.setCitationKey("jabreftest2021");
        expected.setField(StandardField.AUTHOR, "Me, myself and I");
        expected.setField(StandardField.TITLE, "Something");
        expected.setField(StandardField.VOLUME, "1");
        expected.setField(StandardField.JOURNAL, "Some Journal");
        expected.setField(StandardField.YEAR, "2021");
        expected.setFiles(Collections.singletonList(new LinkedFile("", file.toAbsolutePath(), "PDF")));

        List<BibEntry> resultSecondImport = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        assertEquals(Collections.singletonList(expected), result);
        assertEquals(Collections.singletonList(expected), resultSecondImport);
    }
}
