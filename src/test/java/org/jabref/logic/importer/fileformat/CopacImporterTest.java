package org.jabref.logic.importer.fileformat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.util.FileType;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CopacImporterTest {

    private CopacImporter importer;

    @BeforeEach
    public void setUp() throws Exception {
        importer = new CopacImporter();
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileType.COPAC, importer.getFileType());

    }

    @Test
    public void testGetDescription() {
        assertEquals("Importer for COPAC format.", importer.getDescription());
    }


    @Test
    public void testImportEmptyEntries() throws Exception {
        Path path = Paths.get(CopacImporterTest.class.getResource("Empty.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(path, StandardCharsets.UTF_8).getDatabase().getEntries();
        assertEquals(Collections.emptyList(), entries);
    }
}
