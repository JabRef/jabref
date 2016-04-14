package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RISImporterTest {

    private RisImporter risImporter;


    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        risImporter = new RisImporter();
    }

    @Test
    public void testGetFormatName() {
        Assert.assertEquals(risImporter.getFormatName(), "RIS");
    }

    @Test
    public void testGetCLIId() {
        Assert.assertEquals(risImporter.getId(), "ris");
    }

    @Test
    public void testIfNotRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Paths.get(RISImporterTest.class.getResource("RisImporterCorrupted.ris").toURI());
        Assert.assertFalse(risImporter.isRecognizedFormat(file, Charset.defaultCharset()));
    }

}
