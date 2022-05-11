package org.jabref.logic.xmp;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.transform.TransformerException;

import org.jabref.logic.bibtex.comparator.IdComparator;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.jabref.logic.xmp.DublinCoreExtractor.DC_COVERAGE;
import static org.jabref.logic.xmp.DublinCoreExtractor.DC_RIGHTS;
import static org.jabref.logic.xmp.DublinCoreExtractor.DC_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XmpUtilWriterTest {

    private final BibEntry olly2018 = new BibEntry(StandardEntryType.Article)
            .withCitationKey("Olly2018")
            .withField(StandardField.AUTHOR, "Olly and Johannes")
            .withField(StandardField.TITLE, "Stefan's palace")
            .withField(StandardField.JOURNAL, "Test Journal")
            .withField(StandardField.VOLUME, "1")
            .withField(StandardField.NUMBER, "1")
            .withField(StandardField.PAGES, "1-2")
            .withMonth(Month.MARCH)
            .withField(StandardField.ISSN, "978-123-123")
            .withField(StandardField.NOTE, "NOTE")
            .withField(StandardField.ABSTRACT, "ABSTRACT")
            .withField(StandardField.COMMENT, "COMMENT")
            .withField(StandardField.DOI, "10/3212.3123")
            .withField(StandardField.FILE, ":article_dublinCore.pdf:PDF")
            .withField(StandardField.GROUPS, "NO")
            .withField(StandardField.HOWPUBLISHED, "online")
            .withField(StandardField.KEYWORDS, "k1, k2")
            .withField(StandardField.OWNER, "me")
            .withField(StandardField.REVIEW, "review")
            .withField(StandardField.URL, "https://www.olly2018.edu");
    ;
    private final BibEntry toral2006 = new BibEntry(StandardEntryType.InProceedings)
            .withField(StandardField.AUTHOR, "Antonio Toral and Rafael Munoz")
            .withField(StandardField.TITLE, "A proposal to automatically build and maintain gazetteers for Named Entity Recognition by using Wikipedia")
            .withField(StandardField.BOOKTITLE, "Proceedings of EACL")
            .withField(StandardField.PAGES, "56--61")
            .withField(StandardField.EPRINTTYPE, "asdf")
            .withField(StandardField.OWNER, "Ich")
            .withField(StandardField.URL, "www.url.de");
    private final BibEntry vapnik2000 = new BibEntry(StandardEntryType.Book)
            .withCitationKey("vapnik2000")
            .withField(StandardField.TITLE, "The Nature of Statistical Learning Theory")
            .withField(StandardField.PUBLISHER, "Springer Science + Business Media")
            .withField(StandardField.AUTHOR, "Vladimir N. Vapnik")
            .withField(StandardField.DOI, "10.1007/978-1-4757-3264-1")
            .withField(StandardField.OWNER, "Ich")
            .withField(StandardField.LANGUAGE, "English, Japanese")
            .withDate(new Date(2000, 5))
            .withField(new UnknownField(DC_COVERAGE), "coverageField")
            .withField(new UnknownField((DC_SOURCE)), "JabRef")
            .withField(new UnknownField(DC_RIGHTS), "Right To X");
    private XmpPreferences xmpPreferences;

    @BeforeEach
    void setUp() {
        xmpPreferences = mock(XmpPreferences.class);
        when(xmpPreferences.getKeywordSeparator()).thenReturn(',');
        // The code assumes privacy filters to be off
        when(xmpPreferences.shouldUseXmpPrivacyFilter()).thenReturn(false);
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
        new XmpUtilWriter().writeXmp(pdfFile.toAbsolutePath(), entry, null, xmpPreferences);

        // read entry again
        List<BibEntry> entriesWritten = new XmpUtilReader().readXmp(pdfFile, xmpPreferences);
        BibEntry entryWritten = entriesWritten.get(0);
        entryWritten.clearField(StandardField.FILE);

        assertEquals(List.of(entry), entriesWritten);
    }

    @Test
    void testWriteTwoBibEntries(@TempDir Path tempDir) throws IOException, TransformerException {
        Path pdfFile = this.createDefaultFile("JabRef_writeMultiple.pdf", tempDir);
        List<BibEntry> entries = List.of(olly2018, toral2006);
        new XmpUtilWriter().writeXmp(Path.of(pdfFile.toAbsolutePath().toString()), entries, null, xmpPreferences);
        List<BibEntry> entryList = new XmpUtilReader().readXmp(pdfFile.toAbsolutePath(), xmpPreferences);

        // the file field is not written - and the read file field contains the PDF file name
        // thus, we do not need to compare
        entries.forEach(entry -> entry.clearField(StandardField.FILE));
        entryList.forEach(entry -> entry.clearField(StandardField.FILE));

        assertEquals(entries, entryList);
    }

    @Test
    void testWriteThreeBibEntries(@TempDir Path tempDir) throws IOException, TransformerException {
        Path pdfFile = this.createDefaultFile("JabRef_writeMultiple.pdf", tempDir);
        List<BibEntry> entries = List.of(olly2018, vapnik2000, toral2006);
        new XmpUtilWriter().writeXmp(Path.of(pdfFile.toAbsolutePath().toString()), entries, null, xmpPreferences);
        List<BibEntry> entryList = new XmpUtilReader().readXmp(pdfFile.toAbsolutePath(), xmpPreferences);

        // the file field is not written - and the read file field contains the PDF file name
        // thus, we do not need to compare
        entries.forEach(entry -> entry.clearField(StandardField.FILE));
        entryList.forEach(entry -> entry.clearField(StandardField.FILE));

        assertEquals(entries, entryList);
    }

    /**
     * Creates a temporary PDF-file with a single empty page.
     */
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
