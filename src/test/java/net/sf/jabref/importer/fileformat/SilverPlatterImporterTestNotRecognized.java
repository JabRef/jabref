package net.sf.jabref.importer.fileformat;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

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
            try (InputStream stream = SilverPlatterImporter.class.getResourceAsStream(s)) {
                Assert.assertFalse(testImporter.isRecognizedFormat(stream));
            }
        }
    }

}
