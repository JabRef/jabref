package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
public class PdfGrobidImporterTest {

    private PdfGrobidImporter importer;
    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    public void setUp() {
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importerPreferences.isGrobidEnabled()).thenReturn(true);
        when(importerPreferences.getGrobidURL()).thenReturn("http://grobid.jabref.org:8070");
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        importer = new PdfGrobidImporter(importerPreferences, importFormatPreferences);
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(StandardFileType.PDF, importer.getFileType());
    }

    @Test
    public void testImportEntries() throws URISyntaxException {
        Path file = Path.of(PdfGrobidImporterTest.class.getResource("LNCS-minimal.pdf").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        assertEquals(1, bibEntries.size());

        BibEntry be0 = bibEntries.get(0);
        assertEquals(Optional.of("Lastname, Firstname"), be0.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Paper Title"), be0.getField(StandardField.TITLE));
    }

    @Test
    public void testIsRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Path.of(PdfGrobidImporterTest.class.getResource("annotated.pdf").toURI());
        assertTrue(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException, URISyntaxException {
        Path file = Path.of(PdfGrobidImporterTest.class.getResource("BibtexImporter.examples.bib").toURI());
        assertFalse(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
    }

    @Test
    public void testGetCommandLineId() {
        assertEquals("grobidPdf", importer.getId());
    }
}
