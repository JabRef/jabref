package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.GrobidPreferences;
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

    @BeforeEach
    public void setUp() {
        GrobidPreferences grobidPreferences = mock(GrobidPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(grobidPreferences.isGrobidEnabled()).thenReturn(true);
        when(grobidPreferences.getGrobidURL()).thenReturn("http://grobid.jabref.org:8070");

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(importFormatPreferences.grobidPreferences()).thenReturn(grobidPreferences);

        importer = new PdfGrobidImporter(importFormatPreferences);
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(StandardFileType.PDF, importer.getFileType());
    }

    @Test
    public void testImportEntries() throws URISyntaxException {
        Path file = Path.of(PdfGrobidImporterTest.class.getResource("LNCS-minimal.pdf").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();

        assertEquals(1, bibEntries.size());

        BibEntry be0 = bibEntries.get(0);
        assertEquals(Optional.of("Lastname, Firstname"), be0.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Paper Title"), be0.getField(StandardField.TITLE));
    }

    @Test
    public void testIsRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Path.of(PdfGrobidImporterTest.class.getResource("annotated.pdf").toURI());
        assertTrue(importer.isRecognizedFormat(file));
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException, URISyntaxException {
        Path file = Path.of(PdfGrobidImporterTest.class.getResource("BibtexImporter.examples.bib").toURI());
        assertFalse(importer.isRecognizedFormat(file));
    }

    @Test
    public void testGetCommandLineId() {
        assertEquals("grobidPdf", importer.getId());
    }
}
