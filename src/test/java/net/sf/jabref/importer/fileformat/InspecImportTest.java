package net.sf.jabref.importer.fileformat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibEntryAssert;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

        BibEntry expectedEntry = new BibEntry();
        expectedEntry.setType("article");
        expectedEntry.setField("title", "The SIS project : software reuse with a natural language approach");
        expectedEntry.setField("author", "Prechelt, Lutz");
        expectedEntry.setField("year", "1992");
        expectedEntry.setField("abstract", "Abstrakt");
        expectedEntry.setField("keywords", "key");
        expectedEntry.setField("journal", "10000");
        expectedEntry.setField("pages", "20");
        expectedEntry.setField("volume", "19");

        BibEntryAssert.assertEquals(Collections.singletonList(expectedEntry),
                InspecImportTest.class.getResourceAsStream("InspecImportTest2.txt"), inspecImp);
    }

    @Test
    public void importConferencePaperGivesInproceedings() throws IOException {
        String testInput = "Record.*INSPEC.*\n" +
                "\n" +
                "RT ~ Conference-Paper";
        BibEntry expectedEntry = new BibEntry();
        expectedEntry.setType("Inproceedings");

        try (InputStream inStream = new ByteArrayInputStream(testInput.getBytes())) {
            List<BibEntry> entries = inspecImp.importEntries(inStream, new OutputPrinterToNull());
            assertEquals(Collections.singletonList(expectedEntry), entries);
        }
    }

    @Test
    public void importMiscGivesMisc() throws IOException {
        String testInput = "Record.*INSPEC.*\n" +
                "\n" +
                "RT ~ Misc";
        BibEntry expectedEntry = new BibEntry();
        expectedEntry.setType("Misc");

        try (InputStream inStream = new ByteArrayInputStream(testInput.getBytes())) {
            List<BibEntry> entries = inspecImp.importEntries(inStream, new OutputPrinterToNull());
            assertEquals(1, entries.size());
            BibEntry entry = entries.get(0);
            assertEquals(expectedEntry, entry);
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
