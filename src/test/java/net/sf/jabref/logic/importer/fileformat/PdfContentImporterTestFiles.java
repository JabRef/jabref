package net.sf.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.sf.jabref.logic.bibtex.BibEntryAssert;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PdfContentImporterTestFiles {

    @Parameter
    public String fileName;


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
    public void correctContent() throws IOException, URISyntaxException {
        String pdfFileName = fileName + ".pdf";
        String bibFileName = fileName + ".bib";
        PdfContentImporter importer = new PdfContentImporter(
                JabRefPreferences.getInstance().getImportFormatPreferences());
        Path pdfFile = Paths.get(PdfContentImporter.class.getResource(pdfFileName).toURI());
        List<BibEntry> result = importer.importDatabase(pdfFile, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntryAssert.assertEquals(PdfContentImporterTest.class, bibFileName, result);
    }

}
