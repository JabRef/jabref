package net.sf.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.bibtex.BibEntryAssert;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OvidImporterTest {

    private OvidImporter importer;


    @Before
    public void setUp() {
        importer = new OvidImporter();
    }

    @Test
    public void testGetFormatName() {
        Assert.assertEquals("Ovid", importer.getFormatName());
    }

    @Test
    public void testGetCLIId() {
        Assert.assertEquals("ovid", importer.getId());
    }

    @Test
    public void testsGetExtensions() {
        Assert.assertEquals(FileExtensions.OVID, importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        Assert.assertEquals("Imports an Ovid file.", importer.getDescription());
    }

    @Test
    public void testIsRecognizedFormatAccept() throws IOException, URISyntaxException {

        List<String> list = Arrays.asList("OvidImporterTest1.txt", "OvidImporterTest3.txt", "OvidImporterTest4.txt",
                "OvidImporterTest5.txt", "OvidImporterTest6.txt", "OvidImporterTest7.txt");

        for (String str : list) {
            Path file = Paths.get(OvidImporter.class.getResource(str).toURI());
            Assert.assertTrue(importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public void testIsRecognizedFormatRejected() throws IOException, URISyntaxException {

        List<String> list = Arrays.asList("Empty.txt", "OvidImporterTest2.txt");

        for (String str : list) {
            Path file = Paths.get(OvidImporter.class.getResource(str).toURI());
            Assert.assertFalse(importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public void testImportEmpty() throws IOException, URISyntaxException {
        Path file = Paths.get(OvidImporter.class.getResource("Empty.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();
        Assert.assertEquals(Collections.emptyList(), entries);
    }

    @Test
    public void testImportEntries1() throws IOException, URISyntaxException {
        Path file = Paths.get(OvidImporter.class.getResource("OvidImporterTest1.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();
        Assert.assertEquals(5, entries.size());

        BibEntry entry = entries.get(0);
        Assert.assertEquals("misc", entry.getType());
        Assert.assertEquals(Optional.of("Mustermann and Musterfrau"), entry.getFieldOptional("author"));
        Assert.assertEquals(Optional.of("Short abstract"), entry.getFieldOptional("abstract"));
        Assert.assertEquals(Optional.of("Musterbuch"), entry.getFieldOptional("title"));
        Assert.assertEquals(Optional.of("Einleitung"), entry.getFieldOptional("chaptertitle"));

        entry = entries.get(1);
        Assert.assertEquals("inproceedings", entry.getType());
        Assert.assertEquals(Optional.of("Max"), entry.getFieldOptional("editor"));
        Assert.assertEquals(Optional.of("Max the Editor"), entry.getFieldOptional("title"));
        Assert.assertEquals(Optional.of("Very Long Title"), entry.getFieldOptional("journal"));
        Assert.assertEquals(Optional.of("28"), entry.getFieldOptional("volume"));
        Assert.assertEquals(Optional.of("2"), entry.getFieldOptional("issue"));
        Assert.assertEquals(Optional.of("2015"), entry.getFieldOptional("year"));
        Assert.assertEquals(Optional.of("103--106"), entry.getFieldOptional("pages"));

        entry = entries.get(2);
        Assert.assertEquals("incollection", entry.getType());
        Assert.assertEquals(Optional.of("Max"), entry.getFieldOptional("author"));
        Assert.assertEquals(Optional.of("Test"), entry.getFieldOptional("title"));
        Assert.assertEquals(Optional.of("Very Long Title"), entry.getFieldOptional("journal"));
        Assert.assertEquals(Optional.of("28"), entry.getFieldOptional("volume"));
        Assert.assertEquals(Optional.of("2"), entry.getFieldOptional("issue"));
        Assert.assertEquals(Optional.of("April"), entry.getFieldOptional("month"));
        Assert.assertEquals(Optional.of("2015"), entry.getFieldOptional("year"));
        Assert.assertEquals(Optional.of("103--106"), entry.getFieldOptional("pages"));

        entry = entries.get(3);
        Assert.assertEquals("book", entry.getType());
        Assert.assertEquals(Optional.of("Max"), entry.getFieldOptional("author"));
        Assert.assertEquals(Optional.of("2015"), entry.getFieldOptional("year"));
        Assert.assertEquals(Optional.of("Editor"), entry.getFieldOptional("editor"));
        Assert.assertEquals(Optional.of("Very Long Title"), entry.getFieldOptional("booktitle"));
        Assert.assertEquals(Optional.of("103--106"), entry.getFieldOptional("pages"));
        Assert.assertEquals(Optional.of("Address"), entry.getFieldOptional("address"));
        Assert.assertEquals(Optional.of("Publisher"), entry.getFieldOptional("publisher"));

        entry = entries.get(4);
        Assert.assertEquals("article", entry.getType());
        Assert.assertEquals(Optional.of("2014"), entry.getFieldOptional("year"));
        Assert.assertEquals(Optional.of("58"), entry.getFieldOptional("pages"));
        Assert.assertEquals(Optional.of("Test"), entry.getFieldOptional("address"));
        Assert.assertEquals(Optional.empty(), entry.getFieldOptional("title"));
        Assert.assertEquals(Optional.of("TestPublisher"), entry.getFieldOptional("publisher"));
    }

    @Test
    public void testImportEntries2() throws IOException, URISyntaxException {
        Path file = Paths.get(OvidImporter.class.getResource("OvidImporterTest2.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();
        Assert.assertEquals(Collections.emptyList(), entries);
    }

    @Test
    public void testImportSingleEntries() throws IOException, URISyntaxException {

        for (int n = 3; n <= 7; n++) {
            Path file = Paths.get(OvidImporter.class.getResource("OvidImporterTest" + n + ".txt").toURI());
            try (InputStream nis = OvidImporter.class.getResourceAsStream("OvidImporterTestBib" + n + ".bib")) {
                List<BibEntry> entries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();
                Assert.assertNotNull(entries);
                Assert.assertEquals(1, entries.size());
                BibEntryAssert.assertEquals(nis, entries.get(0));
            }
        }
    }
}
