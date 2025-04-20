package org.jabref.logic.importer.fileformat;

import java.nio.file.Path;
import java.util.Collections;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BiblioscapeImporterTest {

    private BiblioscapeImporter importer;

    @BeforeEach
    void setUp() throws Exception {
        importer = new BiblioscapeImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("Biblioscape", importer.getName());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.TXT, importer.getFileType());
    }

    @Test
    void getDescription() {
        assertEquals("Imports a Biblioscape Tag File.\n" +
                "Several Biblioscape field types are ignored. Others are only included in the BibTeX field \"comment\".", importer.getDescription());
    }

    @Test
    void getCLIID() {
        assertEquals("biblioscape", importer.getId());
    }

    @Test
    void importEntriesAbortion() throws Throwable {
        Path file = Path.of(BiblioscapeImporter.class.getResource("BiblioscapeImporterTestCorrupt.txt").toURI());
        assertEquals(Collections.emptyList(),
                importer.importDatabase(file).getDatabase().getEntries());
    }
}
