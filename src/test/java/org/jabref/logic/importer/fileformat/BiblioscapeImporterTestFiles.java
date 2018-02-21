package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.model.entry.BibEntry;

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
        bsImporter = new BiblioscapeImporter();
    }

    @Parameters(name = "{0}")
    public static Collection<String> fileNames() {
        return Arrays.asList("BiblioscapeImporterTestOptionalFields", "BiblioscapeImporterTestComments",
                "BiblioscapeImporterTestUnknownFields", "BiblioscapeImporterTestKeywords",
                "BiblioscapeImporterTestJournalArticle", "BiblioscapeImporterTestInbook",
                "BiblioscapeImporterTestUnknownType", "BiblioscapeImporterTestArticleST");
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        Assert.assertTrue(bsImporter.isRecognizedFormat(importFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testImportEntries() throws IOException {
        List<BibEntry> bsEntries = bsImporter.importDatabase(importFile, StandardCharsets.UTF_8).getDatabase()
                .getEntries();
        Assert.assertEquals(1, bsEntries.size());
        BibEntryAssert.assertEquals(BiblioscapeImporterTest.class, bibFile, bsEntries);
    }
}
