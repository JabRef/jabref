package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PdfXmpImporterTest {

    private PdfXmpImporter importer;


    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        importer = new PdfXmpImporter();
    }

    @Test
    public void testGetFormatName() {
        assertEquals("XMP-annotated PDF", importer.getFormatName());
    }

    @Test
    public void importEncryptedFileReturnsError() throws IOException, URISyntaxException {
        Path file = Paths.get(PdfXmpImporterTest.class.getResource("/pdfs/encrypted.pdf").toURI());
        ParserResult result = importer.importDatabase(file, Charset.defaultCharset());
        Assert.assertTrue(result.hasWarnings());
    }

    @Test
    public void testImportEntries() throws IOException, URISyntaxException {
        Path file = Paths.get(PdfXmpImporterTest.class.getResource("annotated.pdf").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();

        assertEquals(1, bibEntries.size());

        BibEntry be0 = bibEntries.get(0);
        assertEquals("how to annotate a pdf", be0.getField("abstract"));
        assertEquals("Chris", be0.getField("author"));
        assertEquals("pdf, annotation", be0.getField("keywords"));
        assertEquals("The best Pdf ever", be0.getField("title"));
    }

    @Test
    public void testIsRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Paths.get(PdfXmpImporterTest.class.getResource("annotated.pdf").toURI());
        assertTrue(importer.isRecognizedFormat(file, Charset.defaultCharset()));
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("IEEEImport1.txt", "IsiImporterTest1.isi", "IsiImporterTestInspec.isi",
                "IsiImporterTestWOS.isi", "IsiImporterTestMedline.isi", "RisImporterTest1.ris", "empty.pdf");

        for (String str : list) {
            Path file = Paths.get(PdfXmpImporterTest.class.getResource(str).toURI());
            assertFalse(importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public void testGetCommandLineId() {
        assertEquals("xmp", importer.getId());
    }
}
