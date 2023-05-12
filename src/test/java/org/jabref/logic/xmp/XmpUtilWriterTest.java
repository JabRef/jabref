package org.jabref.logic.xmp;

import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.exporter.XmpExporterTest;
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

/**
 * This tests the writing to a PDF. If the creation of the RDF content should be checked, please head to {@link XmpExporterTest}
 */
class XmpUtilWriterTest {

    @TempDir
    private Path tempDir;

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
            .withField(new UnknownField(DC_SOURCE), "JabRef")
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
    void singleEntryWorks(BibEntry entry) throws Exception {
        Path pdfFile = this.createDefaultFile("JabRef_writeSingle.pdf", tempDir);

        new XmpUtilWriter(xmpPreferences).writeXmp(pdfFile.toAbsolutePath(), entry, null);

        List<BibEntry> entriesWritten = new XmpUtilReader().readXmp(pdfFile, xmpPreferences);

        BibEntry entryWritten = entriesWritten.get(0);
        entryWritten.clearField(StandardField.FILE);
        entry.clearField(StandardField.FILE);

        assertEquals(List.of(entry), entriesWritten);
    }

    @Test
    void olly2018Works() throws Exception {
        singleEntryWorks(olly2018);
    }

    @Test
    void toral2006Works() throws Exception {
        singleEntryWorks(toral2006);
    }

    @Test
    void vapnik2000Works() throws Exception {
        singleEntryWorks(vapnik2000);
    }

    @Test
    void testWriteTwoBibEntries(@TempDir Path tempDir) throws Exception {
        Path pdfFile = this.createDefaultFile("JabRef_writeTwo.pdf", tempDir);
        List<BibEntry> entries = List.of(olly2018, toral2006);
        new XmpUtilWriter(xmpPreferences).writeXmp(pdfFile.toAbsolutePath(), entries, null);
        List<BibEntry> entryList = new XmpUtilReader().readXmp(pdfFile.toAbsolutePath(), xmpPreferences);

        // the file field is not written - and the read file field contains the PDF file name
        // thus, we do not need to compare
        entries.forEach(entry -> entry.clearField(StandardField.FILE));
        entryList.forEach(entry -> entry.clearField(StandardField.FILE));

        assertEquals(entries, entryList);
    }

    @Test
    void testWriteThreeBibEntries(@TempDir Path tempDir) throws Exception {
        Path pdfFile = this.createDefaultFile("JabRef_writeThree.pdf", tempDir);
        List<BibEntry> entries = List.of(olly2018, vapnik2000, toral2006);
        new XmpUtilWriter(xmpPreferences).writeXmp(pdfFile.toAbsolutePath(), entries, null);
        List<BibEntry> entryList = new XmpUtilReader().readXmp(pdfFile.toAbsolutePath(), xmpPreferences);

        // the file field is not written - and the read file field contains the PDF file name
        // thus, we do not need to compare
        entries.forEach(entry -> entry.clearField(StandardField.FILE));
        entryList.forEach(entry -> entry.clearField(StandardField.FILE));

        assertEquals(entries, entryList);
    }

    @Test
    void proctingBracesAreRemovedAtTitle(@TempDir Path tempDir) throws Exception {
        Path pdfFile = this.createDefaultFile("JabRef_writeBraces.pdf", tempDir);
        BibEntry original = new BibEntry()
                .withField(StandardField.TITLE, "Some {P}rotected {T}erm");
        new XmpUtilWriter(xmpPreferences).writeXmp(pdfFile.toAbsolutePath(), List.of(original), null);
        List<BibEntry> entryList = new XmpUtilReader().readXmp(pdfFile.toAbsolutePath(), xmpPreferences);

        entryList.forEach(entry -> entry.clearField(StandardField.FILE));

        BibEntry expected = new BibEntry()
                .withField(StandardField.TITLE, "Some Protected Term");
        assertEquals(List.of(expected), entryList);
    }

    @Test
    void proctingBracesAreKeptAtPages(@TempDir Path tempDir) throws Exception {
        Path pdfFile = this.createDefaultFile("JabRef_writeBraces.pdf", tempDir);
        BibEntry original = new BibEntry()
                .withField(StandardField.PAGES, "{55}-{99}");
        new XmpUtilWriter(xmpPreferences).writeXmp(pdfFile.toAbsolutePath(), List.of(original), null);
        List<BibEntry> entryList = new XmpUtilReader().readXmp(pdfFile.toAbsolutePath(), xmpPreferences);

        entryList.forEach(entry -> entry.clearField(StandardField.FILE));

        assertEquals(List.of(original), entryList);
    }

    @Test
    void doubleDashAtPageNumberIsKept(@TempDir Path tempDir) throws Exception {
        Path pdfFile = this.createDefaultFile("JabRef_writeBraces.pdf", tempDir);
        BibEntry original = new BibEntry()
                .withField(StandardField.PAGES, "2--33");
        new XmpUtilWriter(xmpPreferences).writeXmp(pdfFile.toAbsolutePath(), List.of(original), null);
        List<BibEntry> entryList = new XmpUtilReader().readXmp(pdfFile.toAbsolutePath(), xmpPreferences);

        entryList.forEach(entry -> entry.clearField(StandardField.FILE));

        assertEquals(List.of(original), entryList);
    }

    @Test
    void singleEntry(@TempDir Path tempDir) throws Exception {
        Path pdfFile = this.createDefaultFile("JabRef.pdf", tempDir);
        new XmpUtilWriter(xmpPreferences).writeXmp(pdfFile.toAbsolutePath(), List.of(vapnik2000), null);
        List<BibEntry> entryList = new XmpUtilReader().readXmp(pdfFile.toAbsolutePath(), xmpPreferences);

        vapnik2000.clearField(StandardField.FILE);
        entryList.forEach(entry -> entry.clearField(StandardField.FILE));
        assertEquals(List.of(vapnik2000), entryList);
    }

    /**
     * Creates a temporary PDF-file with a single empty page.
     */
    private Path createDefaultFile(String fileName, Path tempDir) throws Exception {
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
