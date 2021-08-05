package org.jabref.logic.importer.fileformat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PdfMergeMetadataImporterTest {

    private PdfMergeMetadataImporter importer;
    private ImportFormatPreferences importFormatPreferences;
    private XmpPreferences xmpPreferences;

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getFieldContentFormatterPreferences().getNonWrappableFields()).thenReturn(List.of());
        xmpPreferences = mock(XmpPreferences.class);
        importer = new PdfMergeMetadataImporter(importFormatPreferences, xmpPreferences);
    }

    @Test
    void testsGetExtensions() {
        assertEquals(StandardFileType.PDF, importer.getFileType());
    }

    @Test
    void testGetDescription() {
        assertEquals("PdfMergeMetadataImporter imports metadata from a PDF using multiple strategies and merging the result.",
                     importer.getDescription());
    }

    @Test
    void doesNotHandleEncryptedPdfs() throws Exception {
        Path file = Path.of(PdfMergeMetadataImporter.class.getResource("/pdfs/encrypted.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void importWorksAsExpected() throws Exception {
        Path file = Path.of(PdfMergeMetadataImporterTest.class.getResource("mixedMetadata.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        BibEntry expected = new BibEntry(StandardEntryType.Article);
        expected.setCitationKey("jabreftext2021");
        expected.setField(StandardField.AUTHOR, "Donald Knuth");
        expected.setField(StandardField.FILE, ":" + file.toAbsolutePath().toString() + ":" + StandardFileType.PDF.getName());
        expected.setField(StandardField.JOURNAL, "Some Journal");
        expected.setField(StandardField.TITLE, "Knuth: Computers and Typesetting");
        expected.setField(StandardField.URL, "http://www-cs-faculty.stanford.edu/\\~{}uno/abcde.html");
        expected.setField(StandardField.VOLUME, "1");
        expected.setField(StandardField.YEAR, "2021");

        assertEquals(Collections.singletonList(expected), result);
    }
}
