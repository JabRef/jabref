package org.jabref.logic.importer.fileformat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BiblioscapeImporterTest {

    private BiblioscapeImporter importer;

    @BeforeEach
    public void setUp() throws Exception {
        importer = new BiblioscapeImporter();
    }

    @Test
    public void testGetFormatName() {
        assertEquals("Biblioscape", importer.getName());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(StandardFileType.TXT, importer.getFileType());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Imports a Biblioscape Tag File.\n" +
                "Several Biblioscape field types are ignored. Others are only included in the BibTeX field \"comment\".", importer.getDescription());
    }

    @Test
    public void testGetCLIID() {
        assertEquals("biblioscape", importer.getId());
    }

    @Test
    public void testImportEntriesAbortion() throws Throwable {
        Path file = Path.of(BiblioscapeImporter.class.getResource("BiblioscapeImporterTestCorrupt.txt").toURI());
        assertEquals(Collections.emptyList(),
                importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries());
    }
}
