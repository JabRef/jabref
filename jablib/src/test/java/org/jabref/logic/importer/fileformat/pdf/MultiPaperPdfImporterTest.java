package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.collections.FXCollections;

import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.externalfiles.PdfLocationResolver;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDPageLabelRange;
import org.apache.pdfbox.pdmodel.common.PDPageLabels;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class MultiPaperPdfImporterTest {

    @TempDir
    private Path tempDir;

    private final ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private MultiPaperPdfImporter importer;

    @BeforeEach
    void setUp() {
        when(importFormatPreferences.citationKeyPatternPreferences().getUnwantedCharacters()).thenReturn(CitationKeyPatternPreferences.DEFAULT_UNWANTED_CHARACTERS);
        importer = new MultiPaperPdfImporter(importFormatPreferences);
    }

    // [utest->req~logic.importer.multi-paper-pdf~1]
    @Test
    void importsEachPaperAsAnEntryThatCrossReferencesTheWholePdf() throws IOException {
        Path pdf = volume("Proceedings of the Example Conference", true);

        List<BibEntry> entries = importer.importDatabase(pdf).getDatabase().getEntries();

        assertEquals(List.of(
                new BibEntry(StandardEntryType.InProceedings)
                        .withField(StandardField.TITLE, "Alpha")
                        .withField(StandardField.PAGES, "1--2")
                        .withField(StandardField.CROSSREF, "volume")
                        .withField(PdfLocationResolver.PAGE_OFFSET, "2"),
                new BibEntry(StandardEntryType.InProceedings)
                        .withField(StandardField.TITLE, "Beta")
                        .withField(StandardField.PAGES, "3--4")
                        .withField(StandardField.CROSSREF, "volume")
                        .withField(PdfLocationResolver.PAGE_OFFSET, "2"),
                new BibEntry(StandardEntryType.Proceedings)
                        .withCitationKey("volume")
                        .withField(StandardField.TITLE, "volume")
                        .withField(PdfLocationResolver.PAGE_OFFSET, "2")
                        .withFiles(List.of(new LinkedFile("", pdf, StandardFileType.PDF.getName())))),
                entries);
    }

    // [utest->req~logic.importer.multi-paper-pdf~1]
    @Test
    void importedPaperOpensTheWholePdfAtItsFirstPage() throws IOException {
        List<BibEntry> entries = importer.importDatabase(volume("Proceedings of the Example Conference", true)).getDatabase().getEntries();
        BibEntry beta = entries.get(1);
        BibEntry wholePdf = entries.getLast();

        assertEquals(Optional.of(new PdfLocationResolver.PdfLocation(wholePdf.getFiles().getFirst(), Optional.of(5), wholePdf)),
                PdfLocationResolver.resolve(beta, new BibDatabase(entries)));
    }

    // [utest->req~logic.importer.multi-paper-pdf~1]
    @ParameterizedTest
    @MethodSource
    void typesTheWholePdfByItsFrontMatter(String cover, EntryType volumeType, EntryType paperType) throws IOException {
        List<BibEntry> entries = importer.importDatabase(volume(cover, true)).getDatabase().getEntries();

        assertEquals(List.of(paperType, paperType, volumeType), entries.stream().map(BibEntry::getType).toList());
    }

    private static Stream<Arguments> typesTheWholePdfByItsFrontMatter() {
        return Stream.of(
                Arguments.of("Proceedings of the Example Conference", StandardEntryType.Proceedings, StandardEntryType.InProceedings),
                Arguments.of("Example Studies (Eds.)", StandardEntryType.Collection, StandardEntryType.InCollection),
                Arguments.of("An Example Monograph", StandardEntryType.Book, StandardEntryType.InBook));
    }

    // [utest->req~logic.importer.multi-paper-pdf~1]
    @ParameterizedTest
    @MethodSource
    void typesTheWholePdfByItsTitlePagesOutlineAndPapers(List<String> coverLines, String frontMatter, List<String> paperLines, EntryType volumeType) throws IOException {
        List<BibEntry> entries = importer.importDatabase(volume("Example Studies", frontMatter, paperLines, true, coverLines)).getDatabase().getEntries();

        assertEquals(volumeType, entries.getLast().getType());
    }

    private static Stream<Arguments> typesTheWholePdfByItsTitlePagesOutlineAndPapers() {
        return Stream.of(
                Arguments.of(List.of("Proceedings, Part I"), "Contents", List.of(), StandardEntryType.Proceedings),
                Arguments.of(List.of(), "Program Committee", List.of(), StandardEntryType.Proceedings),
                Arguments.of(List.of("Edited by Ada Lovelace"), "Contents", List.of(), StandardEntryType.Collection),
                Arguments.of(List.of(), "List of Contributors", List.of(), StandardEntryType.Collection),
                Arguments.of(List.of(), "Contents", List.of("Ada Lovelace, ada@example.org"), StandardEntryType.Collection),
                Arguments.of(List.of("The series publishes conference and workshop proceedings."), "Contents", List.of(), StandardEntryType.Book));
    }

    // [utest->req~logic.importer.multi-paper-pdf~1]
    @Test
    void autoDetectionImportsThePapersOfAMultiPaperPdf() throws IOException, ImportException {
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importerPreferences.getCustomImporters()).thenReturn(FXCollections.emptyObservableSet());
        ImportFormatReader reader = new ImportFormatReader(importerPreferences, importFormatPreferences, mock(CitationKeyPatternPreferences.class), new DummyFileUpdateMonitor());
        Path pdf = volume("Proceedings of the Example Conference", "Contents", List.of(), true, List.of(
                "@article{one, author = {Ada Lovelace}, title = {One}, journal = {Journal}, year = {2020}, volume = {1}}",
                "@article{two, author = {Ada Lovelace}, title = {Two}, journal = {Journal}, year = {2021}, volume = {2}}",
                "@article{three, author = {Ada Lovelace}, title = {Three}, journal = {Journal}, year = {2022}, volume = {3}}"));

        ImportFormatReader.ImportResult result = reader.importWithAutoDetection(pdf);

        assertEquals(importer.getName(), result.format());
        assertEquals(List.of("Alpha", "Beta", "volume"), result.parserResult().getDatabase().getEntries().stream().map(entry -> entry.getField(StandardField.TITLE).orElseThrow()).toList());
    }

    @Test
    void usesThePdfPagesWhenThePdfHasNoPageLabels() throws IOException {
        Path pdf = volume("Proceedings of the Example Conference", false);

        List<BibEntry> entries = importer.importDatabase(pdf).getDatabase().getEntries();

        assertEquals(List.of("3--4", "5--6"), entries.subList(0, 2).stream().map(entry -> entry.getField(StandardField.PAGES).orElseThrow()).toList());
        assertEquals(Optional.empty(), entries.getLast().getField(PdfLocationResolver.PAGE_OFFSET));
    }

    @Test
    void importsNothingFromAPdfWithoutPapers() throws IOException {
        Path pdf = tempDir.resolve("paper.pdf");
        try (PDDocument document = new PDDocument()) {
            for (int page = 0; page < 3; page++) {
                addPage(document);
            }
            document.save(pdf.toFile());
        }

        assertEquals(List.of(), importer.importDatabase(pdf).getDatabase().getEntries());
    }

    private Path volume(String cover, boolean withPageLabels) throws IOException {
        return volume(cover, "Contents", List.of(), withPageLabels, List.of());
    }

    /// @param paperLines small print on the first page of each paper
    /// @param coverLines small print on the cover
    private Path volume(String cover, String frontMatterBookmark, List<String> paperLines, boolean withPageLabels, List<String> coverLines) throws IOException {
        Path pdf = tempDir.resolve("volume.pdf");
        try (PDDocument document = new PDDocument()) {
            addPage(document, cover, coverLines);
            addPage(document);
            addPage(document, "The paper titled Alpha", paperLines);
            addPage(document);
            addPage(document, "The paper titled Beta", paperLines);
            addPage(document);

            PDOutlineItem alpha = bookmark("Alpha", document.getPage(2));
            alpha.addLast(bookmark("1 Introduction", document.getPage(2)));
            PDOutlineItem beta = bookmark("Beta", document.getPage(4));
            beta.addLast(bookmark("1 Introduction", document.getPage(4)));
            PDDocumentOutline outline = new PDDocumentOutline();
            outline.addLast(bookmark(frontMatterBookmark, document.getPage(1)));
            outline.addLast(alpha);
            outline.addLast(beta);
            document.getDocumentCatalog().setDocumentOutline(outline);

            if (withPageLabels) {
                PDPageLabels labels = new PDPageLabels(document);
                PDPageLabelRange frontMatter = new PDPageLabelRange();
                frontMatter.setStyle(PDPageLabelRange.STYLE_ROMAN_LOWER);
                labels.setLabelItem(0, frontMatter);
                PDPageLabelRange papers = new PDPageLabelRange();
                papers.setStyle(PDPageLabelRange.STYLE_DECIMAL);
                labels.setLabelItem(2, papers);
                document.getDocumentCatalog().setPageLabels(labels);
            }
            document.save(pdf.toFile());
        }
        return pdf;
    }

    private static void addPage(PDDocument document) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            writeBody(content);
        }
    }

    private static void addPage(PDDocument document, String title, List<String> moreLines) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
            content.newLineAtOffset(72, 720);
            content.showText(title);
            content.endText();
            writeBody(content);
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 6);
            for (int line = 0; line < moreLines.size(); line++) {
                content.beginText();
                content.newLineAtOffset(36, 400 - (12 * line));
                content.showText(moreLines.get(line));
                content.endText();
            }
        }
    }

    private static void writeBody(PDPageContentStream content) throws IOException {
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        for (int line = 0; line < 3; line++) {
            content.beginText();
            content.newLineAtOffset(72, 600 - (14 * line));
            content.showText("Body text that sets the most common font size of the page.");
            content.endText();
        }
    }

    private static PDOutlineItem bookmark(String title, PDPage page) {
        PDOutlineItem item = new PDOutlineItem();
        item.setTitle(title);
        item.setDestination(page);
        return item;
    }
}
