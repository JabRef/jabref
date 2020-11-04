package org.jabref.logic.xmp;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XmpUtilWriterTest {

    private static BibEntry olly2018;
    private static BibEntry toral2006;
    private static BibEntry vapnik2000;
    private XmpPreferences xmpPreferences;

    private void initBibEntries() {
        olly2018 = new BibEntry(StandardEntryType.Article);
        olly2018.setCitationKey("Olly2018");
        olly2018.setField(StandardField.AUTHOR, "Olly and Johannes");
        olly2018.setField(StandardField.TITLE, "Stefan's palace");
        olly2018.setField(StandardField.JOURNAL, "Test Journal");
        olly2018.setField(StandardField.VOLUME, "1");
        olly2018.setField(StandardField.NUMBER, "1");
        olly2018.setField(StandardField.PAGES, "1-2");
        olly2018.setMonth(Month.MARCH);
        olly2018.setField(StandardField.ISSN, "978-123-123");
        olly2018.setField(StandardField.NOTE, "NOTE");
        olly2018.setField(StandardField.ABSTRACT, "ABSTRACT");
        olly2018.setField(StandardField.COMMENT, "COMMENT");
        olly2018.setField(StandardField.DOI, "10/3212.3123");
        olly2018.setField(StandardField.FILE, ":article_dublinCore.pdf:PDF");
        olly2018.setField(StandardField.GROUPS, "NO");
        olly2018.setField(StandardField.HOWPUBLISHED, "online");
        olly2018.setField(StandardField.KEYWORDS, "k1, k2");
        olly2018.setField(StandardField.OWNER, "me");
        olly2018.setField(StandardField.REVIEW, "review");
        olly2018.setField(StandardField.URL, "https://www.olly2018.edu");

        toral2006 = new BibEntry(StandardEntryType.InProceedings);
        toral2006.setField(StandardField.AUTHOR, "Toral, Antonio and Munoz, Rafael");
        toral2006.setField(StandardField.TITLE, "A proposal to automatically build and maintain gazetteers for Named Entity Recognition by using Wikipedia");
        toral2006.setField(StandardField.BOOKTITLE, "Proceedings of EACL");
        toral2006.setField(StandardField.PAGES, "56--61");
        toral2006.setField(StandardField.EPRINTTYPE, "asdf");
        toral2006.setField(StandardField.OWNER, "Ich");
        toral2006.setField(StandardField.URL, "www.url.de");

        vapnik2000 = new BibEntry(StandardEntryType.Book);
        vapnik2000.setCitationKey("vapnik2000");
        vapnik2000.setField(StandardField.TITLE, "The Nature of Statistical Learning Theory");
        vapnik2000.setField(StandardField.PUBLISHER, "Springer Science + Business Media");
        vapnik2000.setField(StandardField.AUTHOR, "Vladimir N. Vapnik");
        vapnik2000.setField(StandardField.DOI, "10.1007/978-1-4757-3264-1");
        vapnik2000.setField(StandardField.OWNER, "Ich");
    }

    /**
     * Create a temporary PDF-file with a single empty page.
     */
    @BeforeEach
    void setUp() {
        xmpPreferences = mock(XmpPreferences.class);
        // The code assumes privacy filters to be off
        when(xmpPreferences.shouldUseXmpPrivacyFilter()).thenReturn(false);

        when(xmpPreferences.getKeywordSeparator()).thenReturn(',');

        this.initBibEntries();
    }

    /**
     * Test for writing a PDF file with a single DublinCore metadata entry.
     */
    @Test
    void testWriteXmp(@TempDir Path tempDir) throws IOException, TransformerException {
        Path pdfFile = this.createDefaultFile("JabRef_writeSingle.pdf", tempDir);

        // read a bib entry from the tests before
        BibEntry entry = vapnik2000;
        entry.setCitationKey("WriteXMPTest");
        entry.setId("ID4711");

        // write the changed bib entry to the PDF
        XmpUtilWriter.writeXmp(pdfFile.toAbsolutePath().toString(), entry, null, xmpPreferences);

        // read entry again
        List<BibEntry> entriesWritten = XmpUtilReader.readXmp(pdfFile.toAbsolutePath().toString(), xmpPreferences);
        BibEntry entryWritten = entriesWritten.get(0);
        entryWritten.clearField(StandardField.FILE);

        // compare the two entries
        assertEquals(entry, entryWritten);
    }

    /**
     * Test, which writes multiple metadata entries to a PDF and reads them again to test the size.
     */
    @Test
    void testWriteMultipleBibEntries(@TempDir Path tempDir) throws IOException, TransformerException {
        Path pdfFile = this.createDefaultFile("JabRef_writeMultiple.pdf", tempDir);

        List<BibEntry> entries = Arrays.asList(olly2018, vapnik2000, toral2006);

        XmpUtilWriter.writeXmp(Path.of(pdfFile.toAbsolutePath().toString()), entries, null, xmpPreferences);

        List<BibEntry> entryList = XmpUtilReader.readXmp(Path.of(pdfFile.toAbsolutePath().toString()), xmpPreferences);
        assertEquals(3, entryList.size());
    }

    private Path createDefaultFile(String fileName, Path tempDir) throws IOException {
        // create a default PDF
        Path pdfFile = tempDir.resolve(fileName);
        try (PDDocument pdf = new PDDocument()) {
            // Need a single page to open in Acrobat
            pdf.addPage(new PDPage());
            pdf.save(pdfFile.toAbsolutePath().toString());
        }

        return pdfFile;
    }
}
