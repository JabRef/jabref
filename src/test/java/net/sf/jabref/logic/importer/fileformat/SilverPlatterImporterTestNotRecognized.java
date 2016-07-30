package net.sf.jabref.logic.importer.fileformat;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.importer.fileformat.SilverPlatterImporter;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class SilverPlatterImporterTestNotRecognized {

    public SilverPlatterImporter testImporter;


    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        testImporter = new SilverPlatterImporter();
    }

    @Test
    public final void testIsNotRecognizedFormat() throws Exception {
        List<String> notAccept = Arrays.asList("emptyFile.xml", "IsiImporterTest1.isi",
                "RisImporterTest1.ris", "InspecImportTest2.txt");
        for (String s : notAccept) {
            URL resource = SilverPlatterImporter.class.getResource(s);
            assertNotNull("resource " + s + " must be available", resource);
            Path file = Paths.get(resource.toURI());
            Assert.assertFalse(testImporter.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

}
