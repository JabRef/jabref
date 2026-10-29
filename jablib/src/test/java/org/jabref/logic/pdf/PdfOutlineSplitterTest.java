package org.jabref.logic.pdf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.externalfiles.PdfLocationResolver;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class PdfOutlineSplitterTest {

    @TempDir
    private Path tempDir;

    // [utest->req~logic.pdf.split-proceedings-outline~1]
    // [utest->req~logic.pdf.paper-entry-crossref~1]
    @Test
    void createsOneEntryPerPaperBookmarkAndKeepsTheOriginal() throws IOException {
        Path source = proceedingsWithPapers();
        BibEntry proceedings = new BibEntry(StandardEntryType.Proceedings).withCitationKey("Proc2024");

        List<BibEntry> papers = PdfOutlineSplitter.split(source, proceedings, tempDir).papers();

        assertEquals(2, papers.size());
        assertEquals(StandardEntryType.InProceedings, papers.getFirst().getType());
        assertEquals("Alpha", papers.getFirst().getField(StandardField.TITLE).orElseThrow());
        assertEquals("1--2", papers.getFirst().getField(StandardField.PAGES).orElseThrow());
        assertEquals("Proc2024", papers.getFirst().getField(StandardField.CROSSREF).orElseThrow());
        assertEquals("Beta", papers.get(1).getField(StandardField.TITLE).orElseThrow());
        assertEquals("3--5", papers.get(1).getField(StandardField.PAGES).orElseThrow());
        assertEquals("-2", papers.get(1).getField(PdfLocationResolver.PAGE_OFFSET).orElseThrow());
        assertEquals(2, pageCount(Path.of(papers.getFirst().getFiles().getFirst().getLink())));
        assertEquals(3, pageCount(Path.of(papers.get(1).getFiles().getFirst().getLink())));
        assertEquals(5, pageCount(source));

        BibDatabase database = new BibDatabase();
        database.insertEntries(papers);
        assertEquals(1, PdfLocationResolver.resolve(papers.get(1), database).orElseThrow().page().orElseThrow());
    }

    // [utest->req~logic.pdf.split-proceedings-outline~1]
    @ParameterizedTest
    @MethodSource
    void splitsAVolumeIntoPapersOfItsKind(EntryType volumeType, EntryType paperType) throws IOException {
        List<BibEntry> papers = PdfOutlineSplitter.split(proceedingsWithPapers(), new BibEntry(volumeType), tempDir).papers();

        assertTrue(PdfOutlineSplitter.holdsPapers(volumeType));
        assertEquals(List.of(paperType, paperType), papers.stream().map(BibEntry::getType).toList());
    }

    private static Stream<Arguments> splitsAVolumeIntoPapersOfItsKind() {
        return Stream.of(
                Arguments.of(StandardEntryType.Proceedings, StandardEntryType.InProceedings),
                Arguments.of(StandardEntryType.MvProceedings, StandardEntryType.InProceedings),
                Arguments.of(StandardEntryType.Collection, StandardEntryType.InCollection),
                Arguments.of(StandardEntryType.MvCollection, StandardEntryType.InCollection),
                Arguments.of(StandardEntryType.Book, StandardEntryType.InBook),
                Arguments.of(StandardEntryType.MvBook, StandardEntryType.InBook));
    }

    @Test
    void splitsOnlyVolumes() {
        assertFalse(PdfOutlineSplitter.holdsPapers(StandardEntryType.InProceedings));
        assertFalse(PdfOutlineSplitter.holdsPapers(StandardEntryType.InBook));
        assertFalse(PdfOutlineSplitter.holdsPapers(StandardEntryType.Article));
    }

    @Test
    void usesNestedBookmarksRatherThanTheirParent() throws IOException {
        Path source = tempDir.resolve("nested.pdf");
        try (PDDocument document = new PDDocument()) {
            addPages(document, 4);
            PDOutlineItem part = bookmark("Part", document.getPage(0));
            part.addLast(paper("Alpha", document, 0));
            part.addLast(paper("Beta", document, 2));
            PDDocumentOutline outline = new PDDocumentOutline();
            outline.addLast(part);
            document.getDocumentCatalog().setDocumentOutline(outline);
            document.save(source.toFile());
        }

        List<BibEntry> papers = PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir).papers();

        assertEquals(List.of("Alpha", "Beta"), papers.stream().map(paper -> paper.getField(StandardField.TITLE).orElseThrow()).toList());
    }

    @Test
    void doesNotSplitAPaperIntoItsSections() throws IOException {
        Path source = tempDir.resolve("sections.pdf");
        try (PDDocument document = new PDDocument()) {
            addPages(document, 6);
            PDOutlineItem alpha = paper("Alpha", document, 0);
            alpha.addLast(bookmark("1 Introduction", document.getPage(0)));
            alpha.addLast(bookmark("2 Method", document.getPage(1)));
            PDOutlineItem beta = paper("Beta", document, 3);
            beta.addLast(bookmark("1 Introduction", document.getPage(3)));
            beta.addLast(bookmark("2 Results", document.getPage(4)));
            PDDocumentOutline outline = new PDDocumentOutline();
            outline.addLast(alpha);
            outline.addLast(beta);
            document.getDocumentCatalog().setDocumentOutline(outline);
            document.save(source.toFile());
        }

        List<BibEntry> papers = PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir).papers();

        assertEquals(List.of("Alpha", "Beta"), papers.stream().map(paper -> paper.getField(StandardField.TITLE).orElseThrow()).toList());
        assertEquals(List.of("1--3", "4--6"), papers.stream().map(paper -> paper.getField(StandardField.PAGES).orElseThrow()).toList());
    }

    @Test
    void leavesOutFrontAndBackMatterWithoutSections() throws IOException {
        Path source = tempDir.resolve("volume.pdf");
        try (PDDocument document = new PDDocument()) {
            addPages(document, 7);
            PDOutlineItem alpha = paper("Alpha", document, 1);
            alpha.addLast(bookmark("1 Introduction", document.getPage(1)));
            PDOutlineItem beta = paper("Beta", document, 3);
            beta.addLast(bookmark("1 Introduction", document.getPage(3)));
            PDDocumentOutline outline = new PDDocumentOutline();
            outline.addLast(bookmark("Preface", document.getPage(0)));
            outline.addLast(alpha);
            outline.addLast(beta);
            outline.addLast(bookmark("Author Index", document.getPage(6)));
            document.getDocumentCatalog().setDocumentOutline(outline);
            document.save(source.toFile());
        }

        List<BibEntry> papers = PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir).papers();

        assertEquals(List.of("Alpha", "Beta"), papers.stream().map(paper -> paper.getField(StandardField.TITLE).orElseThrow()).toList());
        assertEquals(List.of("2--3", "4--6"), papers.stream().map(paper -> paper.getField(StandardField.PAGES).orElseThrow()).toList());
    }

    @Test
    void stopsAtAnOutlineThatLinksBackToAnEarlierBookmark() throws IOException {
        Path source = tempDir.resolve("looping.pdf");
        try (PDDocument document = new PDDocument()) {
            addPages(document, 6);
            PDOutlineItem beta = paper("Beta", document, 2);
            PDOutlineItem gamma = paper("Gamma", document, 4);
            PDDocumentOutline outline = new PDDocumentOutline();
            outline.addLast(paper("Alpha", document, 0));
            outline.addLast(beta);
            outline.addLast(gamma);
            gamma.getCOSObject().setItem(COSName.NEXT, beta);
            document.getDocumentCatalog().setDocumentOutline(outline);
            document.save(source.toFile());
        }

        List<BibEntry> papers = PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir).papers();

        assertEquals(List.of("Alpha", "Beta", "Gamma"), papers.stream().map(paper -> paper.getField(StandardField.TITLE).orElseThrow()).toList());
    }

    @Test
    void doesNotSplitASinglePaperWhoseSectionsShareNoSubsection() throws IOException {
        Path source = tempDir.resolve("single.pdf");
        try (PDDocument document = new PDDocument()) {
            addPages(document, 6);
            PDOutlineItem method = paper("2 Method", document, 1);
            method.addLast(bookmark("2.1 Model", document.getPage(1)));
            method.addLast(bookmark("2.2 Restrictions", document.getPage(2)));
            PDOutlineItem results = paper("3 Results", document, 3);
            results.addLast(bookmark("3.1 Returns", document.getPage(3)));
            results.addLast(bookmark("3.2 Flows", document.getPage(4)));
            PDDocumentOutline outline = new PDDocumentOutline();
            outline.addLast(bookmark("1 Introduction", document.getPage(0)));
            outline.addLast(method);
            outline.addLast(results);
            document.getDocumentCatalog().setDocumentOutline(outline);
            document.save(source.toFile());
        }

        assertEquals(List.of(), PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir).papers());
    }

    @Test
    void doesNotSplitWhenTheBookmarksDoNotStartOnTitlePages() throws IOException {
        Path source = tempDir.resolve("flat.pdf");
        try (PDDocument document = new PDDocument()) {
            addPages(document, 4);
            PDDocumentOutline outline = new PDDocumentOutline();
            outline.addLast(bookmark("Introduction", document.getPage(0)));
            outline.addLast(bookmark("Conclusion", document.getPage(2)));
            document.getDocumentCatalog().setDocumentOutline(outline);
            document.save(source.toFile());
        }

        assertEquals(List.of(), PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir).papers());
    }

    // [utest->req~logic.pdf.split-proceedings-outline~1]
    @Test
    void writesThePrintedPagesFromThePageLabels() throws IOException {
        Path source = proceedingsWithPageLabels();

        PdfOutlineSplitter.SplitPapers split = PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir);

        assertEquals(List.of("1--2", "3--4"), split.papers().stream().map(paper -> paper.getField(StandardField.PAGES).orElseThrow()).toList());
        assertEquals(2, split.pageOffset());
    }

    @Test
    void keepsThePageOffsetStoredOnTheParent() throws IOException {
        Path source = proceedingsWithPageLabels();
        BibEntry proceedings = new BibEntry(StandardEntryType.Proceedings).withField(PdfLocationResolver.PAGE_OFFSET, "0");

        PdfOutlineSplitter.SplitPapers split = PdfOutlineSplitter.split(source, proceedings, tempDir);

        assertEquals(List.of("3--4", "5--6"), split.papers().stream().map(paper -> paper.getField(StandardField.PAGES).orElseThrow()).toList());
        assertEquals(0, split.pageOffset());
    }

    // [utest->req~logic.pdf.split-proceedings-outline~1]
    @Test
    void keepsAPaperThatStartsOnThePageOfAPartHeading() throws IOException {
        Path source = tempDir.resolve("parts.pdf");
        try (PDDocument document = new PDDocument()) {
            addPages(document, 4);
            PDOutlineItem alpha = paper("Alpha", document, 0);
            alpha.addLast(bookmark("1 Introduction", document.getPage(0)));
            PDOutlineItem beta = paper("Beta", document, 2);
            beta.addLast(bookmark("1 Introduction", document.getPage(2)));
            PDDocumentOutline outline = new PDDocumentOutline();
            outline.addLast(bookmark("Part I", document.getPage(0)));
            outline.addLast(alpha);
            outline.addLast(beta);
            document.getDocumentCatalog().setDocumentOutline(outline);
            document.save(source.toFile());
        }

        List<BibEntry> papers = PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir).papers();

        assertEquals(List.of("Alpha", "Beta"), papers.stream().map(paper -> paper.getField(StandardField.TITLE).orElseThrow()).toList());
    }

    @Test
    void createsADifferentFileEachTimeForTheSameName() throws IOException {
        Path first = PdfPageCopier.createUniqueFile(tempDir, "Alpha.pdf");
        Path second = PdfPageCopier.createUniqueFile(tempDir, "Alpha.pdf");

        assertNotEquals(first, second);
        assertTrue(Files.exists(first));
        assertTrue(Files.exists(second));
    }

    @Test
    void returnsNothingWhenThePdfHasNoOutline() throws IOException {
        Path source = tempDir.resolve("plain.pdf");
        try (PDDocument document = new PDDocument()) {
            addPages(document, 3);
            document.save(source.toFile());
        }

        assertEquals(List.of(), PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir).papers());
        assertEquals(3, pageCount(source));
    }

    @Test
    void splitsTheCyberPhysicalSystemsOfSystemsProceedings() throws IOException {
        List<String> papers = List.of(
                "Basic Concepts on Systems of Systems",
                "Interfaces in Evolving Cyber-Physical Systems-of-Systems",
                "Emergence in Cyber-Physical Systems-of-Systems (CPSoSs)",
                "AMADEOS SysML Profile for SoS Conceptual Modeling",
                "AMADEOS Framework and Supporting Tools",
                "Time and Resilient Master Clocks in Cyber-Physical Systems",
                "Managing Dynamicity in SoS",
                "Case Study Definition and Implementation");
        Path source = tempDir.resolve("cyber-physical-systems-of-systems.pdf");
        try (PDDocument document = new PDDocument()) {
            addPages(document, 4 + papers.size() + 1);
            PDDocumentOutline outline = new PDDocumentOutline();
            outline.addLast(bookmark("Foreword", document.getPage(0)));
            outline.addLast(bookmark("Preface", document.getPage(1)));
            outline.addLast(bookmark("Acknowledgments", document.getPage(2)));
            outline.addLast(bookmark("Contents", document.getPage(3)));
            for (int index = 0; index < papers.size(); index++) {
                PDOutlineItem paper = paper(papers.get(index), document, 4 + index);
                paper.addLast(bookmark("1 Introduction", document.getPage(4 + index)));
                outline.addLast(paper);
            }
            outline.addLast(bookmark("Author Index", document.getPage(4 + papers.size())));
            document.getDocumentCatalog().setDocumentOutline(outline);
            document.save(source.toFile());
        }

        List<BibEntry> entries = PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir).papers();

        assertEquals(papers, entries.stream().map(entry -> entry.getField(StandardField.TITLE).orElseThrow()).toList());
    }

    private Path proceedingsWithPageLabels() throws IOException {
        Path source = tempDir.resolve("labelled.pdf");
        try (PDDocument document = new PDDocument()) {
            addPages(document, 6);
            PDDocumentOutline outline = new PDDocumentOutline();
            outline.addLast(paper("Alpha", document, 2));
            outline.addLast(paper("Beta", document, 4));
            document.getDocumentCatalog().setDocumentOutline(outline);
            PDPageLabels labels = new PDPageLabels(document);
            PDPageLabelRange frontMatter = new PDPageLabelRange();
            frontMatter.setStyle(PDPageLabelRange.STYLE_ROMAN_LOWER);
            labels.setLabelItem(0, frontMatter);
            PDPageLabelRange papers = new PDPageLabelRange();
            papers.setStyle(PDPageLabelRange.STYLE_DECIMAL);
            labels.setLabelItem(2, papers);
            document.getDocumentCatalog().setPageLabels(labels);
            document.save(source.toFile());
        }
        return source;
    }

    private Path proceedingsWithPapers() throws IOException {
        Path source = tempDir.resolve("proceedings.pdf");
        try (PDDocument document = new PDDocument()) {
            addPages(document, 5);
            PDDocumentOutline outline = new PDDocumentOutline();
            outline.addLast(paper("Alpha", document, 0));
            outline.addLast(paper("Beta", document, 2));
            document.getDocumentCatalog().setDocumentOutline(outline);
            document.save(source.toFile());
        }
        return source;
    }

    private static void addPages(PDDocument document, int count) throws IOException {
        for (int index = 0; index < count; index++) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                for (int line = 0; line < 3; line++) {
                    content.beginText();
                    content.newLineAtOffset(72, 600 - (14 * line));
                    content.showText("Body text that sets the most common font size of the page.");
                    content.endText();
                }
            }
        }
    }

    private static PDOutlineItem paper(String title, PDDocument document, int pageIndex) throws IOException {
        PDPage page = document.getPage(pageIndex);
        try (PDPageContentStream content = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
            content.newLineAtOffset(72, 720);
            content.showText("The paper titled " + title);
            content.endText();
        }
        return bookmark(title, page);
    }

    private static PDOutlineItem bookmark(String title, PDPage page) {
        PDOutlineItem item = new PDOutlineItem();
        item.setTitle(title);
        item.setDestination(page);
        return item;
    }

    private static int pageCount(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            return document.getNumberOfPages();
        }
    }
}
