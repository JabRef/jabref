package net.sf.jabref.importer.fileformat;

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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OvidImporterTest {

    private OvidImporter importer;


    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        importer = new OvidImporter();
    }

    @Test
    public void testGetFormatName() {
        Assert.assertEquals("Ovid", importer.getFormatName());
    }

    @Test
    public void testGetCLIId() {
        Assert.assertEquals("ovid", importer.getCLIId());
    }

    @Test
    public void testIsRecognizedFormatAccept() throws IOException {

        List<String> list = Arrays.asList("OvidImporterTest1.txt", "OvidImporterTest3.txt", "OvidImporterTest4.txt",
                "OvidImporterTest5.txt", "OvidImporterTest6.txt", "OvidImporterTest7.txt");

        for (String str : list) {
            try (InputStream is = OvidImporter.class.getResourceAsStream(str)) {
                Assert.assertTrue(importer.isRecognizedFormat(is));
            }
        }
    }

    @Test
    public void testIsRecognizedFormatRejected() throws IOException {

        List<String> list = Arrays.asList("Empty.txt", "OvidImporterTest2.txt");

        for (String str : list) {
            try (InputStream is = OvidImporter.class.getResourceAsStream(str)) {
                Assert.assertFalse(importer.isRecognizedFormat(is));
            }
        }
    }

    @Test
    public void testImportEmpty() throws IOException {

        try (InputStream is = OvidImporter.class.getResourceAsStream("Empty.txt")) {
            List<BibEntry> entries = importer.importEntries(is, new OutputPrinterToNull());
            Assert.assertEquals(Collections.emptyList(), entries);
        }
    }

    @Test
    public void testImportEntries1() throws IOException {

        try (InputStream is = OvidImporter.class.getResourceAsStream("OvidImporterTest1.txt")) {
            List<BibEntry> entries = importer.importEntries(is, new OutputPrinterToNull());
            Assert.assertEquals(5, entries.size());

            BibEntry entry = entries.get(0);
            Assert.assertEquals("misc", entry.getType());
            Assert.assertEquals("Mustermann and Musterfrau", entry.getField("author"));
            Assert.assertEquals("Short abstract", entry.getField("abstract"));
            Assert.assertEquals("Musterbuch", entry.getField("title"));
            Assert.assertEquals("Einleitung", entry.getField("chaptertitle"));

            entry = entries.get(1);
            Assert.assertEquals("inproceedings", entry.getType());
            Assert.assertEquals("Max", entry.getField("editor"));
            Assert.assertEquals("Max the Editor", entry.getField("title"));
            Assert.assertEquals("Very Long Title", entry.getField("journal"));
            Assert.assertEquals("28", entry.getField("volume"));
            Assert.assertEquals("2", entry.getField("issue"));
            Assert.assertEquals("2015", entry.getField("year"));
            Assert.assertEquals("103--106", entry.getField("pages"));

            entry = entries.get(2);
            Assert.assertEquals("incollection", entry.getType());
            Assert.assertEquals("Max", entry.getField("author"));
            Assert.assertEquals("Test", entry.getField("title"));
            Assert.assertEquals("Very Long Title", entry.getField("journal"));
            Assert.assertEquals("28", entry.getField("volume"));
            Assert.assertEquals("2", entry.getField("issue"));
            Assert.assertEquals("April", entry.getField("month"));
            Assert.assertEquals("2015", entry.getField("year"));
            Assert.assertEquals("103--106", entry.getField("pages"));

            entry = entries.get(3);
            Assert.assertEquals("book", entry.getType());
            Assert.assertEquals("Max", entry.getField("author"));
            Assert.assertEquals("2015", entry.getField("year"));
            Assert.assertEquals("Editor", entry.getField("editor"));
            Assert.assertEquals("Very Long Title", entry.getField("booktitle"));
            Assert.assertEquals("103--106", entry.getField("pages"));
            Assert.assertEquals("Address", entry.getField("address"));
            Assert.assertEquals("Publisher", entry.getField("publisher"));

            entry = entries.get(4);
            Assert.assertEquals("article", entry.getType());
            Assert.assertEquals("2014", entry.getField("year"));
            Assert.assertEquals("58", entry.getField("pages"));
            Assert.assertEquals("Test", entry.getField("address"));
            Assert.assertNull(entry.getField("title"));
            Assert.assertEquals("TestPublisher", entry.getField("publisher"));
        }
    }

    @Test
    public void testImportEntries2() throws IOException {

        try (InputStream is = OvidImporter.class.getResourceAsStream("OvidImporterTest2.txt")) {
            List<BibEntry> entries = importer.importEntries(is, new OutputPrinterToNull());
            Assert.assertEquals(Collections.emptyList(), entries);
        }
    }

    @Test
    public void testImportSingleEntries() throws IOException {

        for (int n = 3; n <= 7; n++) {
            try (InputStream is = OvidImporter.class.getResourceAsStream("OvidImporterTest" + n + ".txt");
                    InputStream nis = OvidImporter.class.getResourceAsStream("OvidImporterTestBib" + n + ".bib")) {
                List<BibEntry> entries = importer.importEntries(is, new OutputPrinterToNull());
                Assert.assertNotNull(entries);
                Assert.assertEquals(1, entries.size());
                BibEntryAssert.assertEquals(nis, entries.get(0));
            }
        }
    }
}