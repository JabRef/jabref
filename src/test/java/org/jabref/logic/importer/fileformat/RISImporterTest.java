package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class RISImporterTest {

    private RisImporter importer;

    @BeforeEach
    public void setUp() {
        importer = new RisImporter();
    }

    @Test
    public void getFormatName() {
        assertEquals("RIS", importer.getName());
    }

    @Test
    public void getCLIId() {
        assertEquals("ris", importer.getId());
    }

    @Test
    public void sGetExtensions() {
        assertEquals(StandardFileType.RIS, importer.getFileType());
    }

    @Test
    public void getDescription() {
        assertEquals("Imports a Biblioscape Tag File.", importer.getDescription());
    }

    @Test
    public void ifNotRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Path.of(RISImporterTest.class.getResource("RisImporterCorrupted.ris").toURI());
        assertFalse(importer.isRecognizedFormat(file));
    }
}
