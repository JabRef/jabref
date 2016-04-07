package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
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
public class MsBibImporterTestfiles {

    @Parameter
    public String fileName;


    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> fileNames() {
        Object[][] data = new Object[][] {{"MsBibImporterTest1"}, {"MsBibImporterTest2"}, {"MsBibImporterTest3"},
                {"MsBibImporterTest4"}, {"MsBibImporterTest5"}, {"MsBibImporterTest6"}};
        return Arrays.asList(data);
    }


    @Test
    public final void testIsRecognizedFormat() throws Exception {
        String xmlFileName = fileName + ".xml";
        MsBibImporter testImporter = new MsBibImporter();
        try (InputStream stream = MsBibImporter.class.getResourceAsStream(xmlFileName)) {
            Assert.assertTrue(testImporter.isRecognizedFormat(stream));
        }
    }


    @Test
    public void testImportEntries() throws IOException {
        String xmlFileName = fileName + ".xml";
        String bibFileName = fileName + ".bib";
        MsBibImporter testImporter = new MsBibImporter();
        try (InputStream is = MsBibImporter.class.getResourceAsStream(xmlFileName)) {
            List<BibEntry> result = testImporter.importEntries(is, new OutputPrinterToNull());
            BibtexEntryAssert.assertEquals(MsBibImporterTest.class, bibFileName, result);
        }
    }

}
