package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RISImporterTest {

    private RisImporter importer;

    @BeforeEach
    void setUp() {
        importer = new RisImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("RIS", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("ris", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.RIS, importer.getFileType());
    }

    @Test
    void getDescription() {
        assertEquals("Imports a Biblioscape Tag File.", importer.getDescription());
    }

    @Test
    void ifNotRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Path.of(RISImporterTest.class.getResource("RisImporterCorrupted.ris").toURI());
        assertFalse(importer.isRecognizedFormat(file));
    }
}
