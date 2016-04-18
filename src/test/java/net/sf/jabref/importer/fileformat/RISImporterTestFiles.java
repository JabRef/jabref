package net.sf.jabref.importer.fileformat;

import net.sf.jabref.*;
import net.sf.jabref.bibtex.BibtexEntryAssert;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class RISImporterTestFiles {

    private RisImporter risImporter;

    @Parameter
    public String fileName;


    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        risImporter = new RisImporter();
    }

    @Parameters(name = "{0}")
    public static Collection<String> fileNames() {
        return Arrays
                .asList(new String[] {"RisImporterTest1", "RisImporterTest3", "RisImporterTest4a", "RisImporterTest4b",
                        "RisImporterTest4c", "RisImporterTest5a", "RisImporterTest5b", "RisImporterTest6"});
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        try (InputStream stream = RISImporterTest.class.getResourceAsStream(fileName + ".ris")) {
            Assert.assertTrue(risImporter.isRecognizedFormat(stream));
        }
    }

    @Test
    public void testImportEntries() throws IOException {
        try (InputStream risStream = RISImporterTest.class.getResourceAsStream(fileName + ".ris")) {

            List<BibEntry> risEntries = risImporter.importEntries(risStream, new OutputPrinterToNull());
            BibtexEntryAssert.assertEquals(RISImporterTest.class, fileName + ".bib", risEntries);

        }
    }
}
