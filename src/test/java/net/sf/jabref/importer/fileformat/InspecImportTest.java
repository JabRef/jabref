package net.sf.jabref.importer.fileformat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.logic.bibtex.BibEntryAssert;
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
    public void testIsRecognizedFormatAccept() throws IOException, URISyntaxException {
        List<String> testList = Arrays.asList("InspecImportTest.txt", "InspecImportTest2.txt");
        for (String str : testList) {
            Path file = Paths.get(InspecImportTest.class.getResource(str).toURI());
            assertTrue(inspecImp.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException, URISyntaxException {
        List<String> testList = Arrays.asList("CopacImporterTest1.txt", "CopacImporterTest2.txt",
                "IEEEImport1.txt", "IsiImporterTest1.isi", "IsiImporterTestInspec.isi", "IsiImporterTestWOS.isi",
                "IsiImporterTestMedline.isi", "RisImporterTest1.ris", "InspecImportTestFalse.txt");
        for (String str : testList) {
            Path file = Paths.get(InspecImportTest.class.getResource(str).toURI());
            assertFalse(inspecImp.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public void testCompleteBibtexEntryOnJournalPaperImport() throws IOException, URISyntaxException {

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
                "RT ~ Conference-Paper\n" +
                "AU ~ Prechelt, Lutz";
        BibEntry expectedEntry = new BibEntry();
        expectedEntry.setType("Inproceedings");
        expectedEntry.setField("author", "Prechelt, Lutz");

        try (BufferedReader reader = new BufferedReader(new StringReader(testInput))) {
            List<BibEntry> entries = inspecImp.importDatabase(reader).getDatabase().getEntries();
            assertEquals(Collections.singletonList(expectedEntry), entries);
        }
    }

    @Test
    public void importMiscGivesMisc() throws IOException {
        String testInput = "Record.*INSPEC.*\n" +
                "\n" +
                "AU ~ Prechelt, Lutz \n" +
                "RT ~ Misc";
        BibEntry expectedEntry = new BibEntry();
        expectedEntry.setType("Misc");
        expectedEntry.setField("author", "Prechelt, Lutz");

        try (BufferedReader reader = new BufferedReader(new StringReader(testInput))) {
            List<BibEntry> entries = inspecImp.importDatabase(reader).getDatabase().getEntries();
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
        assertEquals("inspec", inspecImp.getId());
    }

}
