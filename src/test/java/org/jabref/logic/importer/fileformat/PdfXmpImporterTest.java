package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.FileExtensions;
import org.jabref.logic.xmp.XMPPreferences;
import org.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class PdfXmpImporterTest {

    private PdfXmpImporter importer;


    @Before
    public void setUp() {
        importer = new PdfXmpImporter(mock(XMPPreferences.class));
    }

    @Test
    public void testGetFormatName() {
        assertEquals("XMP-annotated PDF", importer.getName());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileExtensions.XMP, importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Wraps the XMPUtility function to be used as an Importer.", importer.getDescription());
    }

    @Test
    public void importEncryptedFileReturnsError() throws URISyntaxException {
        Path file = Paths.get(PdfXmpImporterTest.class.getResource("/pdfs/encrypted.pdf").toURI());
        ParserResult result = importer.importDatabase(file, StandardCharsets.UTF_8);
        Assert.assertTrue(result.hasWarnings());
    }

    @Test
    public void testImportEntries() throws URISyntaxException {
        Path file = Paths.get(PdfXmpImporterTest.class.getResource("annotated.pdf").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        assertEquals(1, bibEntries.size());

        BibEntry be0 = bibEntries.get(0);
        assertEquals(Optional.of("how to annotate a pdf"), be0.getField("abstract"));
        assertEquals(Optional.of("Chris"), be0.getField("author"));
        assertEquals(Optional.of("pdf, annotation"), be0.getField("keywords"));
        assertEquals(Optional.of("The best Pdf ever"), be0.getField("title"));
    }

    @Test
    public void testIsRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Paths.get(PdfXmpImporterTest.class.getResource("annotated.pdf").toURI());
        assertTrue(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("IEEEImport1.txt", "IsiImporterTest1.isi", "IsiImporterTestInspec.isi",
                "IsiImporterTestWOS.isi", "IsiImporterTestMedline.isi", "RisImporterTest1.ris", "empty.pdf");

        for (String str : list) {
            Path file = Paths.get(PdfXmpImporterTest.class.getResource(str).toURI());
            assertFalse(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testGetCommandLineId() {
        assertEquals("xmp", importer.getId());
    }
}
