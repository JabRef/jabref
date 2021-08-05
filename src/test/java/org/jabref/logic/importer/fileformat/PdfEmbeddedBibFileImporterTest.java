package org.jabref.logic.importer.fileformat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PdfEmbeddedBibFileImporterTest {

    private PdfEmbeddedBibFileImporter importer;
    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getFieldContentFormatterPreferences().getNonWrappableFields()).thenReturn(List.of());
        importer = new PdfEmbeddedBibFileImporter(importFormatPreferences);
    }

    @Test
    void testsGetExtensions() {
        assertEquals(StandardFileType.PDF, importer.getFileType());
    }

    @Test
    void testGetDescription() {
        assertEquals("PdfEmbeddedBibFileImporter imports an embedded Bib-File from the PDF.",
                     importer.getDescription());
    }

    @Test
    void doesNotHandleEncryptedPdfs() throws Exception {
        Path file = Path.of(PdfEmbeddedBibFileImporter.class.getResource("/pdfs/encrypted.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void importWorksAsExpected() throws Exception {
        Path file = Path.of(PdfEmbeddedBibFileImporterTest.class.getResource("mixedMetadata.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        BibEntry expected = new BibEntry(StandardEntryType.Misc);
        expected.setCitationKey("jabreftext2021");
        expected.setField(StandardField.AUTHOR, "Someone embedded");
        expected.setField(StandardField.TITLE, "I like beds");
        expected.setField(StandardField.DOI, "10.1002/9781118257517");
        expected.setField(StandardField.COMMENT, "From embedded bib");

        assertEquals(Collections.singletonList(expected), result);
    }
}
