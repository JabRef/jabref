package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibEntryAssert;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PdfContentImporterTestFiles {

    @Parameter
    public String fileName;

    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> fileNames() {
        // The test folder contains pairs of PDFs and BibTeX files. We check each pair.
        // This method returns the basenames of the available pairs

        Object[][] data = new Object[][] {
                // minimal PDF, not encrypted
                {"LNCS-minimal"},
                // minimal PDF, write-protected, thus encrypted
                {"LNCS-minimal-protected"}};
        return Arrays.asList(data);
    }

    @Test
    public void correctContent() throws IOException {
        String pdfFileName = fileName + ".pdf";
        String bibFileName = fileName + ".bib";
        PdfContentImporter importer = new PdfContentImporter();
        try (InputStream is = PdfContentImporter.class.getResourceAsStream(pdfFileName)) {
            List<BibEntry> result = importer.importEntries(is, null);
            BibEntryAssert.assertEquals(PdfContentImporterTest.class, bibFileName, result);
        }
    }

}
