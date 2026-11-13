package org.jabref.logic.xmp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.transform.TransformerException;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.jabref.logic.xmp.DublinCoreExtractor.DC_COVERAGE;
import static org.jabref.logic.xmp.DublinCoreExtractor.DC_RIGHTS;
import static org.jabref.logic.xmp.DublinCoreExtractor.DC_SOURCE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// This tests the writing to a PDF. If the creation of the RDF content should be checked, please head to [org.jabref.logic.exporter.XmpExporterTest]
@Execution(ExecutionMode.SAME_THREAD)
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

    /// Test for writing a PDF file with a single DublinCore metadata entry.
    void singleEntryWorks(BibEntry entry) throws IOException, TransformerException {
        Path pdfFile = this.createDefaultFile("JabRef_writeSingle.pdf", tempDir);

        new XmpUtilWriter(xmpPreferences).writeXmp(pdfFile.toAbsolutePath(), entry, null);

        List<BibEntry> entriesWritten = new XmpUtilReader().readXmp(pdfFile, xmpPreferences);

        BibEntry entryWritten = entriesWritten.getFirst();
        entryWritten.clearField(StandardField.FILE);
        entry.clearField(StandardField.FILE);

        assertEquals(List.of(entry), entriesWritten);
    }

    @Test
    void olly2018Works() throws IOException, TransformerException {
        singleEntryWorks(olly2018);
    }

    @Test
    void toral2006Works() throws IOException, TransformerException {
        singleEntryWorks(toral2006);
    }

    @Test
    void vapnik2000Works() throws IOException, TransformerException {
        singleEntryWorks(vapnik2000);
    }

    @Test
    void writeTwoBibEntries(@TempDir Path tempDir) throws IOException, TransformerException {
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
    void writeThreeBibEntries(@TempDir Path tempDir) throws IOException, TransformerException {
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
    void proctingBracesAreRemovedAtTitle(@TempDir Path tempDir) throws IOException, TransformerException {
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
    void proctingBracesAreKeptAtPages(@TempDir Path tempDir) throws IOException, TransformerException {
        Path pdfFile = this.createDefaultFile("JabRef_writeBraces.pdf", tempDir);
        BibEntry original = new BibEntry()
                .withField(StandardField.PAGES, "{55}-{99}");
        new XmpUtilWriter(xmpPreferences).writeXmp(pdfFile.toAbsolutePath(), List.of(original), null);
        List<BibEntry> entryList = new XmpUtilReader().readXmp(pdfFile.toAbsolutePath(), xmpPreferences);

        entryList.forEach(entry -> entry.clearField(StandardField.FILE));

        assertEquals(List.of(original), entryList);
    }

    @Test
    void doubleDashAtPageNumberIsKept(@TempDir Path tempDir) throws IOException, TransformerException {
        Path pdfFile = this.createDefaultFile("JabRef_writeBraces.pdf", tempDir);
        BibEntry original = new BibEntry()
                .withField(StandardField.PAGES, "2--33");
        new XmpUtilWriter(xmpPreferences).writeXmp(pdfFile.toAbsolutePath(), List.of(original), null);
        List<BibEntry> entryList = new XmpUtilReader().readXmp(pdfFile.toAbsolutePath(), xmpPreferences);

        entryList.forEach(entry -> entry.clearField(StandardField.FILE));

        assertEquals(List.of(original), entryList);
    }

    @Test
    void singleEntry(@TempDir Path tempDir) throws IOException, TransformerException {
        Path pdfFile = this.createDefaultFile("JabRef.pdf", tempDir);
        new XmpUtilWriter(xmpPreferences).writeXmp(pdfFile.toAbsolutePath(), List.of(vapnik2000), null);
        List<BibEntry> entryList = new XmpUtilReader().readXmp(pdfFile.toAbsolutePath(), xmpPreferences);

        vapnik2000.clearField(StandardField.FILE);
        entryList.forEach(entry -> entry.clearField(StandardField.FILE));
        assertEquals(List.of(vapnik2000), entryList);
    }

    @Test
        // [utest->req~logic.xmp.atomic-pdf-write~1]
    void writeAndRemoveLeaveLoadablePdfAndNoTempFiles(@TempDir Path tempDir) throws IOException, TransformerException {
        Path pdfFile = this.createDefaultFile("JabRef_atomic.pdf", tempDir);

        new XmpUtilWriter(xmpPreferences).writeXmp(pdfFile.toAbsolutePath(), List.of(olly2018), null);
        try (PDDocument document = Loader.loadPDF(pdfFile.toFile())) {
            assertEquals(1, document.getNumberOfPages());
        }

        XmpUtilWriter.removeXmpMetadata(pdfFile.toAbsolutePath());
        try (PDDocument document = Loader.loadPDF(pdfFile.toFile())) {
            assertEquals(1, document.getNumberOfPages());
        }

        try (Stream<Path> files = Files.list(tempDir)) {
            assertEquals(List.of(pdfFile), files.toList());
        }
    }

    @Test
        // [utest->req~logic.xmp.atomic-pdf-write~1]
    void failedWriteLeavesOriginalUntouchedAndNoTempFiles(@TempDir Path tempDir) throws IOException, URISyntaxException {
        Path pdfFile = tempDir.resolve("encrypted.pdf");
        Files.copy(Path.of(XmpUtilWriterTest.class.getResource("/pdfs/encrypted.pdf").toURI()), pdfFile);
        byte[] originalBytes = Files.readAllBytes(pdfFile);

        assertThrows(IOException.class,
                () -> new XmpUtilWriter(xmpPreferences).writeXmp(pdfFile.toAbsolutePath(), List.of(olly2018), null));

        assertArrayEquals(originalBytes, Files.readAllBytes(pdfFile));
        try (Stream<Path> files = Files.list(tempDir)) {
            assertEquals(List.of(pdfFile), files.toList());
        }
    }

    private static Stream<Arguments> latexTextFields() {
        return Stream.of("title", "author", "editor", "abstract", "keywords", "publisher", "coverage", "rights", "source")
                     .flatMap(field -> Stream.of(
                             Arguments.of(FieldFactory.parseField(field), "M\\\"uller", "Müller"),
                             Arguments.of(FieldFactory.parseField(field), "Research \\& Development", "Research & Development")));
    }

    @ParameterizedTest
    @MethodSource("latexTextFields")
    void exportsLatexAsPlainTextInDublinCore(Field field, String original, String plainText) throws IOException, TransformerException {
        Path pdf = createDefaultFile("latex.pdf", tempDir);
        BibEntry entry = new BibEntry().withField(field, original);
        new XmpUtilWriter(xmpPreferences).writeXmp(pdf, entry, new BibDatabase());

        try (PDDocument document = Loader.loadPDF(pdf.toFile());
            var stream = document.getDocumentCatalog().getMetadata().createInputStream()) {
            DublinCoreSchema schema = new XmpUtilShared().parseXmpMetadata(stream).getDublinCoreSchema();
            BibEntry exported = new DublinCoreExtractor(schema, xmpPreferences, new BibEntry()).extractBibtexEntry().orElseThrow();

            assertEquals(plainText, exported.getField(field).orElseThrow());
            assertEquals(original, entry.getField(field).orElseThrow());
        }
    }

    @ParameterizedTest
    @CsvSource({"1\\textsuperscript{st}, 1st", "2\\textsuperscript{nd}, 2nd", "3\\textsuperscript{rd}, 3rd", "10\\textsuperscript{th}, 10th"})
    void exportsOrdinalSuffixesAsPlainText(String original, String plainText) throws IOException, TransformerException {
        exportsLatexAsPlainTextInDublinCore(StandardField.TITLE, original, plainText);
    }

    @Test
    void exportsIssueExampleAsPlainTextInStandardTitle() throws IOException, TransformerException {
        exportsLatexAsPlainTextInDublinCore(StandardField.TITLE,
                "Proceedings of the 10\\textsuperscript{th} Central European Workshop on Services and their Composition ({ZEUS} 2018)",
                "Proceedings of the 10th Central European Workshop on Services and their Composition (ZEUS 2018)");
    }

    @Test
    void standardPdfMetadataContainsPlainText() throws IOException, TransformerException {
        Path pdf = createDefaultFile("plain-text.pdf", tempDir);
        BibEntry entry = new BibEntry()
                .withField(StandardField.TITLE, "The 10\\textsuperscript{th} {CEUR} Workshop")
                .withField(StandardField.AUTHOR, "M{\\\"u}ller")
                .withField(StandardField.ABSTRACT, "Research \\& Development")
                .withField(StandardField.KEYWORDS, "{CEUR}, M{\\\"u}ller");
        new XmpUtilWriter(xmpPreferences).writeXmp(pdf, entry, new BibDatabase());

        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            assertEquals(List.of("The 10th CEUR Workshop", "Müller", "Research & Development", "CEUR, Müller"),
                    List.of(document.getDocumentInformation().getTitle(), document.getDocumentInformation().getAuthor(),
                            document.getDocumentInformation().getSubject(), document.getDocumentInformation().getKeywords()));
        }
    }

    @Test
    void issueExampleKeepsLatexInBibtexFields() throws IOException, TransformerException {
        String booktitle = "Proceedings of the 10\\textsuperscript{th} Central European Workshop on Services and their Composition ({ZEUS} 2018)";
        String series = "{CEUR} Workshop Proceedings";
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.BOOKTITLE, booktitle)
                .withField(StandardField.SERIES, series);
        Path pdf = createDefaultFile("bibtex.pdf", tempDir);
        new XmpUtilWriter(xmpPreferences).writeXmp(pdf, entry, new BibDatabase());

        try (PDDocument document = Loader.loadPDF(pdf.toFile());
            var stream = document.getDocumentCatalog().getMetadata().createInputStream()) {
            DublinCoreSchema schema = new XmpUtilShared().parseXmpMetadata(stream).getDublinCoreSchema();
            assertEquals(List.of("bibtex/booktitle/" + booktitle, "bibtex/series/" + series), schema.getRelations());
            assertEquals(booktitle, document.getDocumentInformation().getCustomMetadataValue("bibtex/booktitle"));
            assertEquals(series, document.getDocumentInformation().getCustomMetadataValue("bibtex/series"));
        }
    }

    /// Creates a temporary PDF-file with a single empty page.
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
