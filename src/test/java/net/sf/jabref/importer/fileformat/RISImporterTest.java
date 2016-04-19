package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;

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
        Assert.assertEquals(risImporter.getCLIId(), "ris");
    }

    @Test
    public void testIfNotRecognizedFormat() throws IOException {
        try (InputStream stream = RISImporterTest.class.getResourceAsStream("RisImporterCorrupted.ris")) {
            Assert.assertFalse(risImporter.isRecognizedFormat(stream));
        }
    }

}
