package net.sf.jabref.importer.fileformat;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.bibtex.BibtexEntryAssert;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

public class InspecImportTest {

    private InspecImporter inspecImp;

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        this.inspecImp = new InspecImporter();
    }

    @Test
    public void testIsRecognizedFormatAccept() throws IOException {
        List<String> testList = Arrays.asList("InspecImportTest.txt", "InspecImportTest2.txt");
        for (String str : testList) {
            try (InputStream inStream = InspecImportTest.class.getResourceAsStream(str)) {
                assertTrue(inspecImp.isRecognizedFormat(inStream));
            }
        }
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException {
        List<String> testList = Arrays.asList("CopacImporterTest1.txt", "CopacImporterTest2.txt",
                "IEEEImport1.txt", "IsiImporterTest1.isi", "IsiImporterTestInspec.isi", "IsiImporterTestWOS.isi",
                "IsiImporterTestMedline.isi", "RisImporterTest1.ris", "InspecImportTestFalse.txt");
        for (String str : testList) {
            try (InputStream inStream = InspecImportTest.class.getResourceAsStream(str)) {
                assertFalse(inspecImp.isRecognizedFormat(inStream));
            }
        }
    }

    @Test
    public void testCompleteBibtexEntryOnJournalPaperImport() throws IOException {

        BibEntry shouldBeEntry = new BibEntry();
        shouldBeEntry.setType("article");
        shouldBeEntry.setField("title", "The SIS project : software reuse with a natural language approach");
        shouldBeEntry.setField("author", "Prechelt, Lutz");
        shouldBeEntry.setField("year", "1992");
        shouldBeEntry.setField("abstract", "Abstrakt");
        shouldBeEntry.setField("keywords", "key");
        shouldBeEntry.setField("journal", "10000");
        shouldBeEntry.setField("pages", "20");
        shouldBeEntry.setField("volume", "19");

        try (InputStream inStream = InspecImportTest.class.getResourceAsStream("InspecImportTest2.txt")) {
            List<BibEntry> entries = inspecImp.importEntries(inStream, new OutputPrinterToNull());
            assertEquals(1, entries.size());
            BibEntry entry = entries.get(0);
            BibtexEntryAssert.assertEquals(shouldBeEntry, entry);

        }
    }

    @Test
    public void importConferencePaperGivesInproceedings() throws IOException {
        String testInput = "Record.*INSPEC.*\n" +
                "\n" +
                "RT ~ Conference-Paper";
        BibEntry shouldBeEntry = new BibEntry();
        shouldBeEntry.setType("Inproceedings");

        try (InputStream inStream = new ByteArrayInputStream(testInput.getBytes())) {
            List<BibEntry> entries = inspecImp.importEntries(inStream, new OutputPrinterToNull());
            assertEquals(1, entries.size());
            BibEntry entry = entries.get(0);
            BibtexEntryAssert.assertEquals(shouldBeEntry, entry);
        }
    }

    @Test
    public void importMiscGivesMisc() throws IOException {
        String testInput = "Record.*INSPEC.*\n" +
                "\n" +
                "RT ~ Misc";
        BibEntry shouldBeEntry = new BibEntry();
        shouldBeEntry.setType("Misc");

        try (InputStream inStream = new ByteArrayInputStream(testInput.getBytes())) {
            List<BibEntry> entries = inspecImp.importEntries(inStream, new OutputPrinterToNull());
            assertEquals(1, entries.size());
            BibEntry entry = entries.get(0);
            BibtexEntryAssert.assertEquals(shouldBeEntry, entry);
        }
    }

    @Test
    public void testGetFormatName() {
        assertEquals("INSPEC", inspecImp.getFormatName());
    }

    @Test
    public void testGetCLIId() {
        assertEquals("inspec", inspecImp.getCLIId());
    }

}
