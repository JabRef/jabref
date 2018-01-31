package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jabref.logic.util.FileType;

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
    public void testGetFormatName() {
        assertEquals("RIS", importer.getName());
    }

    @Test
    public void testGetCLIId() {
        assertEquals("ris", importer.getId());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileType.RIS, importer.getFileType());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Imports a Biblioscape Tag File.", importer.getDescription());
    }

    @Test
    public void testIfNotRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Paths.get(RISImporterTest.class.getResource("RisImporterCorrupted.ris").toURI());
        assertFalse(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
    }

}
