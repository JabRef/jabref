package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.bibtex.BibEntryAssert;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class BiblioscapeImporterTestFiles {

    private BiblioscapeImporter bsImporter;

    public Path importFile;
    public String bibFile;

    public BiblioscapeImporterTestFiles(String fileName) throws URISyntaxException {
        importFile = Paths.get(BiblioscapeImporterTest.class.getResource(fileName + ".txt").toURI());
        bibFile = fileName + ".bib";
    }

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        bsImporter = new BiblioscapeImporter();
    }

    @Parameters(name = "{0}")
    public static Collection<String> fileNames() {
        return Arrays.asList(
                "BiblioscapeImporterTestOptionalFields",
                "BiblioscapeImporterTestComments",
                "BiblioscapeImporterTestUnknownFields",
                "BiblioscapeImporterTestKeywords",
                "BiblioscapeImporterTestJournalArticle",
                "BiblioscapeImporterTestInbook",
                "BiblioscapeImporterTestUnknownType",
                "BiblioscapeImporterTestArticleST"
        );
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        Assert.assertTrue(bsImporter.isRecognizedFormat(importFile, Charset.defaultCharset()));
    }

    @Test
    public void testImportEntries() throws IOException {
        List<BibEntry> bsEntries = bsImporter.importDatabase(importFile, Charset.defaultCharset()).getDatabase().getEntries();
        Assert.assertEquals(1, bsEntries.size());
        BibEntryAssert.assertEquals(BiblioscapeImporterTest.class, bibFile, bsEntries);
    }
}
