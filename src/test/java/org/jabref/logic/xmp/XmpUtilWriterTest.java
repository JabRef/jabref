package org.jabref.logic.xmp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XmpUtilWriterTest {

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    private XmpPreferences xmpPreferences;

    private static BibEntry olly2018;
    private static BibEntry toral2006;
    private static BibEntry vapnik2000;

    private void initBibEntries() {

        olly2018 = new BibEntry();
        olly2018.setType("article");
        olly2018.setCiteKey("Olly2018");
        olly2018.setField("author", "Olly and Johannes");
        olly2018.setField("title", "Stefan's palace");
        olly2018.setField("journal", "Test Journal");
        olly2018.setField("volume", "1");
        olly2018.setField("number", "1");
        olly2018.setField("pages", "1-2");
        olly2018.setMonth(Month.MARCH);
        olly2018.setField("issn", "978-123-123");
        olly2018.setField("note", "NOTE");
        olly2018.setField("abstract", "ABSTRACT");
        olly2018.setField("comment", "COMMENT");
        olly2018.setField("doi", "10/3212.3123");
        olly2018.setField("file", ":article_dublinCore.pdf:PDF");
        olly2018.setField("groups", "NO");
        olly2018.setField("howpublished", "online");
        olly2018.setField("keywords", "k1, k2");
        olly2018.setField("owner", "me");
        olly2018.setField("review", "review");
        olly2018.setField("url", "https://www.olly2018.edu");

        toral2006 = new BibEntry();
        toral2006.setType("InProceedings");
        toral2006.setField("author", "Toral, Antonio and Munoz, Rafael");
        toral2006.setField("title", "A proposal to automatically build and maintain gazetteers for Named Entity Recognition by using Wikipedia");
        toral2006.setField("booktitle", "Proceedings of EACL");
        toral2006.setField("pages", "56--61");
        toral2006.setField("eprinttype", "asdf");
        toral2006.setField("owner", "Ich");
        toral2006.setField("url", "www.url.de");

        vapnik2000 = new BibEntry();
        vapnik2000.setType("Book");
        vapnik2000.setCiteKey("vapnik2000");
        vapnik2000.setField("title", "The Nature of Statistical Learning Theory");
        vapnik2000.setField("publisher", "Springer Science + Business Media");
        vapnik2000.setField("author", "Vladimir N. Vapnik");
        vapnik2000.setField("doi", "10.1007/978-1-4757-3264-1");
        vapnik2000.setField("owner", "Ich");
    }

    /**
     * Create a temporary PDF-file with a single empty page.
     */
    @Before
    public void setUp() {

        xmpPreferences = mock(XmpPreferences.class);
        // The code assumes privacy filters to be off
        when(xmpPreferences.isUseXMPPrivacyFilter()).thenReturn(false);

        when(xmpPreferences.getKeywordSeparator()).thenReturn(',');

        this.initBibEntries();
    }

    /**
     * Test for writing a PDF file with a single DublinCore metadata entry.
     */
    @Test
    public void testWriteXmp() throws IOException, TransformerException {

        File pdfFile = this.createDefaultFile("JabRef_writeSingle.pdf");

        // read a bib entry from the tests before
        BibEntry entry = vapnik2000;
        entry.setCiteKey("WriteXMPTest");
        entry.setId("ID4711");

        // write the changed bib entry to the create PDF
        XmpUtilWriter.writeXmp(pdfFile.getAbsolutePath(), entry, null, xmpPreferences);

        // read entry again
        List<BibEntry> entriesWritten = XmpUtilReader.readXmp(pdfFile.getPath(), xmpPreferences);
        BibEntry entryWritten = entriesWritten.get(0);

        // compare the two entries
        Assert.assertEquals(entry, entryWritten);

    }

    /**
     * Test, which writes multiple metadata entries to a PDF and reads them again to test the size.
     */
    @Test
    public void testWriteMultipleBibEntries() throws IOException, TransformerException {

        File pdfFile = this.createDefaultFile("JabRef_writeMultiple.pdf");

        List<BibEntry> entries = Arrays.asList(olly2018, vapnik2000, toral2006);

        XmpUtilWriter.writeXmp(Paths.get(pdfFile.getAbsolutePath()), entries, null, xmpPreferences);

        List<BibEntry> entryList = XmpUtilReader.readXmp(Paths.get(pdfFile.getAbsolutePath()), xmpPreferences);
        Assert.assertEquals(3, entryList.size());

    }

    private File createDefaultFile(String fileName) throws IOException {
        // create a default PDF
        File pdfFile = tempFolder.newFile(fileName);
        try (PDDocument pdf = new PDDocument()) {
            // Need a single page to open in Acrobat
            pdf.addPage(new PDPage());
            pdf.save(pdfFile.getPath());
        }

        return pdfFile;
    }
}
