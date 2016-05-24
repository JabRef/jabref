package net.sf.jabref.importer.fileformat;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SilverPlatterImporterTestNotRecognized {

    public SilverPlatterImporter testImporter;


    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        testImporter = new SilverPlatterImporter();
    }

    @Test
    public final void testIsNotRecognizedFormat() throws Exception {
        List<String> notAccept = Arrays.asList("emptyFile.xml", "IsiImporterTest1.isi", "oai2.xml",
                "RisImporterTest1.ris", "InspecImportTest2.txt");
        for (String s : notAccept) {
            Path file = Paths.get(SilverPlatterImporter.class.getResource(s).toURI());
            Assert.assertFalse(testImporter.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

}
