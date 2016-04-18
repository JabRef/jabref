package net.sf.jabref.importer.fileformat;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.logic.xmp.EncryptedPdfsNotSupportedException;
import net.sf.jabref.model.entry.BibEntry;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

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

    @Test(expected = EncryptedPdfsNotSupportedException.class)
    public void importEncryptedFileThrowsException() throws IOException {
        try (InputStream is = PdfXmpImporterTest.class.getResourceAsStream("/pdfs/encrypted.pdf")) {
            importer.importEntries(is, new OutputPrinterToNull());
        }
    }

    @Test
    public void testImportEntries() throws IOException {
        try (InputStream is = PdfXmpImporterTest.class.getResourceAsStream("annotated.pdf")) {
            List<BibEntry> bibEntries = importer.importEntries(is, new OutputPrinterToNull());

            assertEquals(1, bibEntries.size());

            BibEntry be0 = bibEntries.get(0);
            assertEquals("how to annotate a pdf", be0.getField("abstract"));
            assertEquals("Chris", be0.getField("author"));
            assertEquals("pdf, annotation", be0.getField("keywords"));
            assertEquals("The best Pdf ever", be0.getField("title"));
        }
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        try (InputStream is = PdfXmpImporterTest.class.getResourceAsStream("annotated.pdf")) {
            assertTrue(importer.isRecognizedFormat(is));
        }
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException {
        List<String> list = Arrays.asList("IEEEImport1.txt", "IsiImporterTest1.isi", "IsiImporterTestInspec.isi",
                "IsiImporterTestWOS.isi", "IsiImporterTestMedline.isi", "RisImporterTest1.ris", "empty.pdf");

        for (String str : list) {
            try (InputStream is = PdfXmpImporterTest.class.getResourceAsStream(str)) {
                assertFalse(importer.isRecognizedFormat(is));
            }
        }
    }

    @Test
    public void testGetCommandLineId() {
        assertEquals("xmp", importer.getCommandLineId());
    }
}
