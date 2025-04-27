package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CopacImporterTest {

    private CopacImporter importer;

    @BeforeEach
    void setUp() {
        importer = new CopacImporter();
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.TXT, importer.getFileType());
    }

    @Test
    void getDescription() {
        assertEquals("Importer for COPAC format.", importer.getDescription());
    }

    @Test
    void importEmptyEntries() throws URISyntaxException, IOException {
        Path path = Path.of(CopacImporterTest.class.getResource("Empty.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(path).getDatabase().getEntries();
        assertEquals(List.of(), entries);
    }
}
