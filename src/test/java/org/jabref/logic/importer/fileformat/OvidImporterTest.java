package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.logic.util.FileType;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OvidImporterTest {

    private OvidImporter importer;


    @BeforeEach
    public void setUp() {
        importer = new OvidImporter();
    }

    @Test
    public void testGetFormatName() {
        assertEquals("Ovid", importer.getName());
    }

    @Test
    public void testGetCLIId() {
        assertEquals("ovid", importer.getId());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileType.OVID, importer.getFileType());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Imports an Ovid file.", importer.getDescription());
    }

    @Test
    public void testIsRecognizedFormatAccept() throws IOException, URISyntaxException {

        List<String> list = Arrays.asList("OvidImporterTest1.txt", "OvidImporterTest3.txt", "OvidImporterTest4.txt",
                "OvidImporterTest5.txt", "OvidImporterTest6.txt", "OvidImporterTest7.txt");

        for (String str : list) {
            Path file = Paths.get(OvidImporter.class.getResource(str).toURI());
            assertTrue(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testIsRecognizedFormatRejected() throws IOException, URISyntaxException {

        List<String> list = Arrays.asList("Empty.txt", "OvidImporterTest2.txt");

        for (String str : list) {
            Path file = Paths.get(OvidImporter.class.getResource(str).toURI());
            assertFalse(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testImportEmpty() throws IOException, URISyntaxException {
        Path file = Paths.get(OvidImporter.class.getResource("Empty.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        assertEquals(Collections.emptyList(), entries);
    }

    @Test
    public void testImportEntries1() throws IOException, URISyntaxException {
        Path file = Paths.get(OvidImporter.class.getResource("OvidImporterTest1.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        assertEquals(5, entries.size());

        BibEntry entry = entries.get(0);
        assertEquals("misc", entry.getType());
        assertEquals(Optional.of("Mustermann and Musterfrau"), entry.getField("author"));
        assertEquals(Optional.of("Short abstract"), entry.getField("abstract"));
        assertEquals(Optional.of("Musterbuch"), entry.getField("title"));
        assertEquals(Optional.of("Einleitung"), entry.getField("chaptertitle"));

        entry = entries.get(1);
        assertEquals("inproceedings", entry.getType());
        assertEquals(Optional.of("Max"), entry.getField("editor"));
        assertEquals(Optional.of("Max the Editor"), entry.getField("title"));
        assertEquals(Optional.of("Very Long Title"), entry.getField("journal"));
        assertEquals(Optional.of("28"), entry.getField("volume"));
        assertEquals(Optional.of("2"), entry.getField("issue"));
        assertEquals(Optional.of("2015"), entry.getField("year"));
        assertEquals(Optional.of("103--106"), entry.getField("pages"));

        entry = entries.get(2);
        assertEquals("incollection", entry.getType());
        assertEquals(Optional.of("Max"), entry.getField("author"));
        assertEquals(Optional.of("Test"), entry.getField("title"));
        assertEquals(Optional.of("Very Long Title"), entry.getField("journal"));
        assertEquals(Optional.of("28"), entry.getField("volume"));
        assertEquals(Optional.of("2"), entry.getField("issue"));
        assertEquals(Optional.of("April"), entry.getField("month"));
        assertEquals(Optional.of("2015"), entry.getField("year"));
        assertEquals(Optional.of("103--106"), entry.getField("pages"));

        entry = entries.get(3);
        assertEquals("book", entry.getType());
        assertEquals(Optional.of("Max"), entry.getField("author"));
        assertEquals(Optional.of("2015"), entry.getField("year"));
        assertEquals(Optional.of("Editor"), entry.getField("editor"));
        assertEquals(Optional.of("Very Long Title"), entry.getField("booktitle"));
        assertEquals(Optional.of("103--106"), entry.getField("pages"));
        assertEquals(Optional.of("Address"), entry.getField("address"));
        assertEquals(Optional.of("Publisher"), entry.getField("publisher"));

        entry = entries.get(4);
        assertEquals("article", entry.getType());
        assertEquals(Optional.of("2014"), entry.getField("year"));
        assertEquals(Optional.of("58"), entry.getField("pages"));
        assertEquals(Optional.of("Test"), entry.getField("address"));
        assertEquals(Optional.empty(), entry.getField("title"));
        assertEquals(Optional.of("TestPublisher"), entry.getField("publisher"));
    }

    @Test
    public void testImportEntries2() throws IOException, URISyntaxException {
        Path file = Paths.get(OvidImporter.class.getResource("OvidImporterTest2.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        assertEquals(Collections.emptyList(), entries);
    }

    @Test
    public void testImportSingleEntries() throws IOException, URISyntaxException {

        for (int n = 3; n <= 7; n++) {
            Path file = Paths.get(OvidImporter.class.getResource("OvidImporterTest" + n + ".txt").toURI());
            try (InputStream nis = OvidImporter.class.getResourceAsStream("OvidImporterTestBib" + n + ".bib")) {
                List<BibEntry> entries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase()
                        .getEntries();
                assertNotNull(entries);
                assertEquals(1, entries.size());
                BibEntryAssert.assertEquals(nis, entries.get(0));
            }
        }
    }
}
