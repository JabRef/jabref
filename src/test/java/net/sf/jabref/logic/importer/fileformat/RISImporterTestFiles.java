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
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RISImporterTestFiles {

    private RisImporter risImporter;

    @Parameter
    public String fileName;

    private Path risFile;

    @Before
    public void setUp() throws URISyntaxException {
        risImporter = new RisImporter();
        risFile = Paths.get(RISImporterTest.class.getResource(fileName + ".ris").toURI());
    }

    @Parameters(name = "{0}")
    public static Collection<String> fileNames() {
        return Arrays.asList("RisImporterTest1", "RisImporterTest3", "RisImporterTest4a", "RisImporterTest4b",
                        "RisImporterTest4c", "RisImporterTest5a", "RisImporterTest5b", "RisImporterTest6");
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        Assert.assertTrue(risImporter.isRecognizedFormat(risFile, Charset.defaultCharset()));
    }

    @Test
    public void testImportEntries() throws IOException {
        List<BibEntry> risEntries = risImporter.importDatabase(risFile, Charset.defaultCharset()).getDatabase().getEntries();
        BibEntryAssert.assertEquals(RISImporterTest.class, fileName + ".bib", risEntries);
    }
}
