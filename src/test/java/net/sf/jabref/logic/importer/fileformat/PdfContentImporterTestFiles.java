package net.sf.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibLatexEntryTypes;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class PdfContentImporterTestFiles {

    @Parameter public String pdfFileName;
    @Parameter(value = 1) public BibEntry expectedEntry;

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> fileNames() {
        Object[][] data = new Object[][] {
                // minimal PDF, not encrypted
                {"LNCS-minimal.pdf",
                        new BibEntry(BibLatexEntryTypes.ARTICLE)
                                .withField("abstract", "Abstract goes here Simple Figure Simple Table Figure 1. Simple Figure Table 1. Simple Table") // expected: Abstract goes here
                                .withField("title", "Firstname Lastname and Firstname Lastname") // expected: Paper Title and the return as author
                },
                // minimal PDF, write-protected, thus encrypted
                {"LNCS-minimal-protected.pdf",
                        new BibEntry(BibLatexEntryTypes.ARTICLE)
                                .withField("abstract", "Abstract goes here Simple Figure Simple Table Figure 1. Simple Figure Table 1. Simple Table") // expected: Abstract goes here
                                .withField("title", "Firstname Lastname and Firstname Lastname") // expected: Paper Title and the return as author
                },
                {"1405.2249v1.pdf",
                        new BibEntry(BibLatexEntryTypes.ARTICLE) // expected: thesis
                                .withField("author", "Master's Thesis and Presented by Tobias Diez and Assessors:  Dr. G. Rudolph  Dr. R. Verch") // expected: Tobias Diez
                                .withField("pages", "86127") // expected: 1 -- 127
                                .withField("title", "Slice theorem for Fréchet group actions and covariant symplectic field theory")
                }
        };
        return Arrays.asList(data);
    }

    @Test
    public void correctContent() throws IOException, URISyntaxException {
        PdfContentImporter importer = new PdfContentImporter(new ImportFormatPreferences());
        Path pdfFile = Paths.get(PdfContentImporter.class.getResource(pdfFileName).toURI());
        List<BibEntry> result = importer.importDatabase(pdfFile, StandardCharsets.UTF_8).getDatabase().getEntries();
        assertEquals(Collections.singletonList(expectedEntry), result);
    }

}
