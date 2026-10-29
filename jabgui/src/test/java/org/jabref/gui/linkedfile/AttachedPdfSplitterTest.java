package org.jabref.gui.linkedfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.externalfiles.PdfLocationResolver;
import org.jabref.logic.externalfiles.PdfLocationResolver.PdfLocation;
import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.Loader;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

@NullMarked
class AttachedPdfSplitterTest {

    @TempDir
    private Path tempDir;

    private final List<LinkedFile> linkedPapers = new ArrayList<>();

    // [utest->req~logic.pdf.extract-paper-page-range~1]
    @Test
    void attachingAPdfToAnEntryWithPagesLinksACopyOfItsPages() throws IOException {
        Path pdf = blankPdf(5);
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings).withField(StandardField.PAGES, "2--3");

        splitter(library(BibDatabaseMode.BIBLATEX, entry)).split(pdf, entry, linkedPapers::add);

        assertEquals(List.of("proceedings-p2-3.pdf"), linkedPapers.stream().map(AttachedPdfSplitterTest::fileName).toList());
        assertEquals(2, pageCount(tempDir.resolve("proceedings-p2-3.pdf")));
    }

    // [utest->req~logic.pdf.split-proceedings-outline~1]
    // [utest->req~logic.pdf.extract-paper-page-range~1]
    @Test
    void paperGivenItsProceedingsPdfTakesOverTheirPageOffset() throws IOException {
        Path pdf = blankPdf(20);
        BibEntry proceedings = new BibEntry(StandardEntryType.Proceedings)
                .withCitationKey("volume")
                .withField(PdfLocationResolver.PAGE_OFFSET, "14");
        BibEntry paper = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.PAGES, "2--3")
                .withField(StandardField.CROSSREF, "volume")
                .withFiles(List.of(new LinkedFile("", pdf, "PDF")));
        BibDatabaseContext databaseContext = library(BibDatabaseMode.BIBLATEX, proceedings, paper);

        splitter(databaseContext).split(pdf, paper, paper::addFile);

        assertEquals(List.of("proceedings.pdf", "proceedings-p16-17.pdf"), paper.getFiles().stream().map(AttachedPdfSplitterTest::fileName).toList());
        assertEquals(Optional.of(16), PdfLocationResolver.resolve(paper, databaseContext.getDatabase()).flatMap(PdfLocation::page));
    }

    // [utest->req~logic.pdf.split-proceedings-outline~1]
    @Test
    void attachingAPdfToProceedingsCreatesAnEntryPerPaper() throws IOException {
        BibEntry proceedings = new BibEntry(StandardEntryType.Proceedings);
        BibDatabaseContext databaseContext = library(BibDatabaseMode.BIBLATEX, proceedings);

        splitter(databaseContext).split(volumePdf(18, false), proceedings, linkedPapers::add);

        assertEquals(List.of("Alpha", "Beta"), titles(databaseContext));
        assertEquals("Alpha.pdf", fileName(databaseContext.getDatabase().getEntries().get(1)));
        assertEquals(List.of(), linkedPapers);
    }

    @Test
    void doesNotSplitAPdfThatHoldsOnePaper() throws IOException {
        BibEntry proceedings = new BibEntry(StandardEntryType.Proceedings);
        BibDatabaseContext databaseContext = library(BibDatabaseMode.BIBLATEX, proceedings);

        splitter(databaseContext).split(volumePdf(10, false), proceedings, linkedPapers::add);

        assertEquals(List.of(proceedings), databaseContext.getDatabase().getEntries());
    }

    // [utest->req~logic.pdf.split-proceedings-outline~1]
    @Test
    void attachingAPdfToProceedingsStoresThePageOffsetOfThePrintedPages() throws IOException {
        BibEntry proceedings = new BibEntry(StandardEntryType.Proceedings);
        BibDatabaseContext databaseContext = library(BibDatabaseMode.BIBLATEX, proceedings);

        splitter(databaseContext).split(volumePdf(18, true), proceedings, linkedPapers::add);

        assertEquals(List.of("3--4", "5--6"), papers(databaseContext).stream().map(paper -> paper.getField(StandardField.PAGES).orElseThrow()).toList());
        assertEquals(Optional.of("-2"), proceedings.getField(PdfLocationResolver.PAGE_OFFSET));
    }

    // [utest->req~logic.pdf.split-proceedings-outline~1]
    @Test
    void attachingAPdfToProceedingsGivesPapersAlreadyInTheLibraryTheirSplitPdf() throws IOException {
        BibEntry alpha = paper(StandardEntryType.InProceedings, "Alpha", "3--4");
        BibEntry proceedings = new BibEntry(StandardEntryType.Proceedings).withCitationKey("volume");
        BibDatabaseContext databaseContext = library(BibDatabaseMode.BIBLATEX, alpha, proceedings);

        splitter(databaseContext).split(volumePdf(18, true), proceedings, linkedPapers::add);

        assertEquals(List.of("Alpha", "Beta"), titles(databaseContext));
        assertEquals(List.of("Alpha.pdf"), alpha.getFiles().stream().map(AttachedPdfSplitterTest::fileName).toList());
        assertEquals(Optional.of(1), PdfLocationResolver.resolve(alpha, databaseContext.getDatabase()).flatMap(PdfLocation::page));
    }

    // [utest->req~logic.pdf.split-proceedings-outline~1]
    @ParameterizedTest
    @MethodSource
    void attachingAPdfToAVolumeCreatesPapersOfItsKind(EntryType volumeType, BibDatabaseMode mode, EntryType paperType) throws IOException {
        BibEntry volume = new BibEntry(volumeType).withCitationKey("volume").withField(StandardField.TITLE, "Example Volume");
        BibDatabaseContext databaseContext = library(mode, volume);

        splitter(databaseContext).split(volumePdf(18, false), volume, linkedPapers::add);

        assertEquals(List.of(paperType, paperType), papers(databaseContext).stream().map(BibEntry::getType).toList());
    }

    private static Stream<Arguments> attachingAPdfToAVolumeCreatesPapersOfItsKind() {
        return Stream.of(
                Arguments.of(StandardEntryType.Collection, BibDatabaseMode.BIBLATEX, StandardEntryType.InCollection),
                Arguments.of(StandardEntryType.Book, BibDatabaseMode.BIBLATEX, StandardEntryType.InBook),
                Arguments.of(StandardEntryType.Book, BibDatabaseMode.BIBTEX, StandardEntryType.InCollection),
                Arguments.of(StandardEntryType.Proceedings, BibDatabaseMode.BIBTEX, StandardEntryType.InProceedings));
    }

    // [utest->req~logic.pdf.split-proceedings-outline~1]
    @Test
    void splittingInABibtexLibraryGivesTheProceedingsABooktitle() throws IOException {
        BibEntry proceedings = new BibEntry(StandardEntryType.Proceedings)
                .withCitationKey("volume")
                .withField(StandardField.TITLE, "Proceedings of the Example Conference");

        splitter(library(BibDatabaseMode.BIBTEX, proceedings)).split(volumePdf(18, false), proceedings, linkedPapers::add);

        assertEquals(Optional.of("Proceedings of the Example Conference"), proceedings.getField(StandardField.BOOKTITLE));
    }

    // [utest->req~logic.importer.multi-paper-pdf~1]
    @Test
    void splittingImportedVolumeGivesEachImportedPaperItsPages() throws IOException {
        BibEntry alpha = paper(StandardEntryType.InCollection, "Alpha", "1--2");
        BibEntry beta = paper(StandardEntryType.InCollection, "Beta", "3--4");
        BibDatabaseContext databaseContext = library(BibDatabaseMode.BIBLATEX, alpha, beta, volume(StandardEntryType.Collection));

        splitter(databaseContext).splitImportedVolumes(databaseContext.getDatabase().getEntries());

        assertEquals(3, databaseContext.getDatabase().getEntryCount());
        assertEquals(List.of("Alpha.pdf", "Beta.pdf"), List.of(fileName(alpha), fileName(beta)));
        assertEquals(Optional.of(1), PdfLocationResolver.resolve(beta, databaseContext.getDatabase()).flatMap(PdfLocation::page));
    }

    // [utest->req~logic.importer.multi-paper-pdf~1]
    @Test
    void splittingImportedBookInABibtexLibraryPreparesItsPapers() throws IOException {
        BibEntry alpha = paper(StandardEntryType.InBook, "Alpha", "1--2");
        BibEntry beta = paper(StandardEntryType.InBook, "Beta", "3--4");
        BibEntry book = volume(StandardEntryType.Book).withField(StandardField.TITLE, "An Example Monograph");
        BibDatabaseContext databaseContext = library(BibDatabaseMode.BIBTEX, alpha, beta, book);

        splitter(databaseContext).splitImportedVolumes(databaseContext.getDatabase().getEntries());

        assertEquals(List.of(StandardEntryType.InCollection, StandardEntryType.InCollection), List.of(alpha.getType(), beta.getType()));
        assertEquals(Optional.of("An Example Monograph"), book.getField(StandardField.BOOKTITLE));
    }

    @Test
    void doesNotSplitTheVolumeOfImportedPapersThatHaveFiles() throws IOException {
        BibEntry alpha = paper(StandardEntryType.InCollection, "Alpha", "1--2")
                .withFiles(List.of(new LinkedFile("", Path.of("alpha.pdf"), "PDF")));
        BibDatabaseContext databaseContext = library(BibDatabaseMode.BIBLATEX, alpha, volume(StandardEntryType.Collection));

        splitter(databaseContext).splitImportedVolumes(databaseContext.getDatabase().getEntries());

        assertEquals(List.of("alpha.pdf"), alpha.getFiles().stream().map(LinkedFile::getLink).toList());
        assertFalse(Files.exists(tempDir.resolve("Alpha.pdf")));
    }

    private static BibEntry paper(EntryType type, String title, String pages) {
        return new BibEntry(type)
                .withField(StandardField.TITLE, title)
                .withField(StandardField.PAGES, pages)
                .withField(StandardField.CROSSREF, "volume");
    }

    private BibEntry volume(EntryType type) throws IOException {
        return new BibEntry(type)
                .withCitationKey("volume")
                .withFiles(List.of(new LinkedFile("", volumePdf(18, false), "PDF")));
    }

    private static BibDatabaseContext library(BibDatabaseMode mode, BibEntry... entries) {
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(List.of(entries)));
        databaseContext.setMode(mode);
        return databaseContext;
    }

    private static AttachedPdfSplitter splitter(BibDatabaseContext databaseContext) {
        return new AttachedPdfSplitter(databaseContext, mock(DialogService.class), mock(FilePreferences.class), new CurrentThreadTaskExecutor(), new JabRefUndoManager());
    }

    private static List<BibEntry> papers(BibDatabaseContext databaseContext) {
        return databaseContext.getDatabase().getEntries().stream().filter(entry -> entry.hasField(StandardField.PAGES)).toList();
    }

    private static List<String> titles(BibDatabaseContext databaseContext) {
        return papers(databaseContext).stream().map(entry -> entry.getField(StandardField.TITLE).orElseThrow()).toList();
    }

    private Path blankPdf(int pages) throws IOException {
        Path pdf = tempDir.resolve("proceedings.pdf");
        try (PDDocument document = new PDDocument()) {
            for (int page = 0; page < pages; page++) {
                document.addPage(new PDPage());
            }
            document.save(pdf.toFile());
        }
        return pdf;
    }

    private Path volumePdf(int headingSize, boolean numberedFromThree) throws IOException {
        Path pdf = tempDir.resolve("volume.pdf");
        try (PDDocument document = new PDDocument()) {
            for (int page = 0; page < 4; page++) {
                PDPage pdfPage = new PDPage();
                document.addPage(pdfPage);
                try (PDPageContentStream content = new PDPageContentStream(document, pdfPage)) {
                    if (page % 2 == 0) {
                        content.beginText();
                        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), headingSize);
                        content.newLineAtOffset(72, 720);
                        content.showText("A paper in the volume");
                        content.endText();
                    }
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    content.newLineAtOffset(72, 600);
                    content.showText("Body text that sets the most common font size of the page.");
                    content.endText();
                }
            }
            PDDocumentOutline outline = new PDDocumentOutline();
            outline.addLast(bookmark("Alpha", document.getPage(0)));
            outline.addLast(bookmark("Beta", document.getPage(2)));
            document.getDocumentCatalog().setDocumentOutline(outline);
            if (numberedFromThree) {
                PDPageLabels labels = new PDPageLabels(document);
                PDPageLabelRange decimal = new PDPageLabelRange();
                decimal.setStyle(PDPageLabelRange.STYLE_DECIMAL);
                decimal.setStart(3);
                labels.setLabelItem(0, decimal);
                document.getDocumentCatalog().setPageLabels(labels);
            }
            document.save(pdf.toFile());
        }
        return pdf;
    }

    private static PDOutlineItem bookmark(String title, PDPage page) {
        PDOutlineItem item = new PDOutlineItem();
        item.setTitle(title);
        item.setDestination(page);
        return item;
    }

    private static int pageCount(Path pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            return document.getNumberOfPages();
        }
    }

    private static String fileName(BibEntry entry) {
        return fileName(entry.getFiles().getFirst());
    }

    private static String fileName(LinkedFile file) {
        return Path.of(file.getLink()).getFileName().toString();
    }
}
