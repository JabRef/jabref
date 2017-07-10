package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jabref.logic.util.FileExtensions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RISImporterTest {

    private RisImporter importer;


    @Before
    public void setUp() {
        importer = new RisImporter();
    }

    @Test
    public void testGetFormatName() {
        Assert.assertEquals("RIS", importer.getName());
    }

    @Test
    public void testGetCLIId() {
        Assert.assertEquals("ris", importer.getId());
    }

    @Test
    public void testsGetExtensions() {
        Assert.assertEquals(FileExtensions.RIS, importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        Assert.assertEquals("Imports a Biblioscape Tag File.", importer.getDescription());
    }

    @Test
    public void testIfNotRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Paths.get(RISImporterTest.class.getResource("RisImporterCorrupted.ris").toURI());
        Assert.assertFalse(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
    }

}
