package org.jabref.logic.importer.fileformat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class PdfMergeMetadataImporterTest {

    private PdfMergeMetadataImporter importer;
    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setUp() {
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importerPreferences.isGrobidEnabled()).thenReturn(true);
        when(importerPreferences.getGrobidURL()).thenReturn("http://grobid.jabref.org:8070");
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getFieldContentFormatterPreferences().getNonWrappableFields()).thenReturn(List.of());
        importer = new PdfMergeMetadataImporter(importerPreferences, importFormatPreferences);
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

        // From DOI (contained in embedded bib file)
        BibEntry expected = new BibEntry(StandardEntryType.Book);
        expected.setCitationKey("9780134685991");
        expected.setField(StandardField.AUTHOR, "Bloch, Joshua");
        expected.setField(StandardField.TITLE, "Effective Java");
        expected.setField(StandardField.PUBLISHER, "Addison Wesley");
        expected.setField(StandardField.YEAR, "2018");
        expected.setField(StandardField.MONTH, "jul");
        expected.setField(StandardField.DOI, "10.1002/9781118257517");

        // From ISBN (contained on first page verbatim bib entry)
        expected.setField(StandardField.DATE, "2018-01-31");
        expected.setField(new UnknownField("ean"), "9780134685991");
        expected.setField(StandardField.ISBN, "0134685997");
        expected.setField(StandardField.URL, "https://www.ebook.de/de/product/28983211/joshua_bloch_effective_java.html");

        // From embedded bib file
        expected.setField(StandardField.COMMENT, "From embedded bib");

        // From first page verbatim bib entry
        expected.setField(StandardField.JOURNAL, "Some Journal");
        expected.setField(StandardField.VOLUME, "1");

        // From merge
        expected.setFiles(List.of(new LinkedFile("", file.toAbsolutePath(), StandardFileType.PDF.getName())));

        assertEquals(Collections.singletonList(expected), result);
    }
}
