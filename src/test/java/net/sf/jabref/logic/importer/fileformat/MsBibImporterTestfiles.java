package net.sf.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.sf.jabref.logic.bibtex.BibEntryAssert;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MsBibImporterTestfiles {

    @Parameter
    public String fileName;

    private Path xmlFile;
    private ImportFormatPreferences importFormatPreferences;

    @Before
    public void setUp() throws URISyntaxException {
        importFormatPreferences = ImportFormatPreferences.fromPreferences(JabRefPreferences.getInstance());
        xmlFile = Paths.get(MsBibImporter.class.getResource(fileName + ".xml").toURI());
    }

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> fileNames() {
        Object[][] data = new Object[][] {{"MsBibImporterTest1"}, {"MsBibImporterTest2"}, {"MsBibImporterTest3"},
                {"MsBibImporterTest4"}, {"MsBibImporterTest5"}, {"MsBibImporterTest6"}, {"MsBibLCID"}};
        return Arrays.asList(data);
    }


    @Test
    public final void testIsRecognizedFormat() throws Exception {
        MsBibImporter testImporter = new MsBibImporter(importFormatPreferences);
        Assert.assertTrue(testImporter.isRecognizedFormat(xmlFile, Charset.defaultCharset()));
    }


    @Test
    public void testImportEntries() throws IOException {

        String bibFileName = fileName + ".bib";
        MsBibImporter testImporter = new MsBibImporter(importFormatPreferences);
        List<BibEntry> result = testImporter.importDatabase(xmlFile, Charset.defaultCharset()).getDatabase().getEntries();
        BibEntryAssert.assertEquals(MsBibImporterTest.class, bibFileName, result, importFormatPreferences);
    }

}
