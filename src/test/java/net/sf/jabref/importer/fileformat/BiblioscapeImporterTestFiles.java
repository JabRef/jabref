package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibEntryAssert;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class BiblioscapeImporterTestFiles {

    private BiblioscapeImporter bsImporter;

    @Parameter
    public String fileName;


    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        bsImporter = new BiblioscapeImporter();
    }

    @Parameters(name = "{0}")
    public static Collection<String> fileNames() {
        return Arrays.asList(new String[] {
                "BiblioscapeImporterTestOptionalFields",
                "BiblioscapeImporterTestComments",
                "BiblioscapeImporterTestUnknownFields",
                "BiblioscapeImporterTestKeywords",
                "BiblioscapeImporterTestJournalArticle",
                "BiblioscapeImporterTestInbook",
                "BiblioscapeImporterTestUnknownType",
                "BiblioscapeImporterTestArticleST",
                });
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        try (InputStream stream = BiblioscapeImporterTest.class.getResourceAsStream(fileName + ".txt")) {
            Assert.assertTrue(bsImporter.isRecognizedFormat(stream));
        }
    }

    @Test
    public void testImportEntries() throws IOException {
        try (InputStream bsStream = BiblioscapeImporterTest.class.getResourceAsStream(fileName + ".txt")) {

            List<BibEntry> bsEntries = bsImporter.importEntries(bsStream, new OutputPrinterToNull());
            Assert.assertEquals(1, bsEntries.size());
            BibEntryAssert.assertEquals(BiblioscapeImporterTest.class, fileName + ".bib", bsEntries);


        }
    }
}