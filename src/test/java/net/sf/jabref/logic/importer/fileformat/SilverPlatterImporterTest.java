package net.sf.jabref.logic.importer.fileformat;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.bibtex.BibEntryAssert;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SilverPlatterImporterTest {

    private SilverPlatterImporter testImporter;

    @Parameter
    public String filename;

    public Path txtFile;
    public String bibName;


    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        testImporter = new SilverPlatterImporter();
        txtFile = Paths.get(SilverPlatterImporterTest.class.getResource(filename + ".txt").toURI());
        bibName = filename + ".bib";
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileExtensions.SILVER_PLATTER, testImporter.getExtensions());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Imports a SilverPlatter exported file.", testImporter.getDescription());
    }

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> fileNames() {
        Object[][] data = new Object[][] {{"SilverPlatterImporterTest1"}, {"SilverPlatterImporterTest2"}};
        return Arrays.asList(data);
    }

    @Test
    public final void testIsRecognizedFormat() throws Exception {
        Assert.assertTrue(testImporter.isRecognizedFormat(txtFile, Charset.defaultCharset()));
    }

    @Test
    public final void testImportEntries() throws Exception {
        try (InputStream bibIn = SilverPlatterImporterTest.class.getResourceAsStream(bibName)) {
            List<BibEntry> entries = testImporter.importDatabase(txtFile, Charset.defaultCharset()).getDatabase().getEntries();
            BibEntryAssert.assertEquals(bibIn, entries);
        }
    }
}
