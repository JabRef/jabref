package net.sf.jabref.importer.fileformat;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibtexEntryAssert;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;

@RunWith(Parameterized.class)
public class SilverPlatterImporterTest {

    private SilverPlatterImporter testImporter;

    @Parameter
    public String filename;

    public String txtName;
    public String bibName;


    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        testImporter = new SilverPlatterImporter();
        txtName = filename + ".txt";
        bibName = filename + ".bib";
    }

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> fileNames() {
        Object[][] data = new Object[][] {{"SilverPlatterImporterTest1"}, {"SilverPlatterImporterTest2"}};
        return Arrays.asList(data);
    }

    @Test
    public final void testIsRecognizedFormat() throws Exception {
        try (InputStream stream = SilverPlatterImporterTest.class.getResourceAsStream(txtName)) {
            Assert.assertTrue(testImporter.isRecognizedFormat(stream));
        }
    }

    @Test
    public final void testImportEntries() throws Exception {
        try (InputStream in = SilverPlatterImporter.class.getResourceAsStream(txtName);
                InputStream bibIn = SilverPlatterImporterTest.class.getResourceAsStream(bibName)) {
            List<BibEntry> entries = testImporter.importEntries(in, new OutputPrinterToNull());
            BibtexEntryAssert.assertEquals(bibIn, entries);
        }
    }
}
