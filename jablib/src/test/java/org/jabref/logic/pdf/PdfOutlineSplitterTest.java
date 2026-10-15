package org.jabref.logic.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.externalfiles.PdfLocationResolver;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfOutlineSplitterTest {

    @TempDir
    private Path tempDir;

    @Test
    void createsOneEntryPerPaperBookmarkAndKeepsTheOriginal() throws IOException {
        Path source = proceedingsWithPapers();
        BibEntry proceedings = new BibEntry(StandardEntryType.Proceedings).withCitationKey("Proc2024");

        List<BibEntry> papers = PdfOutlineSplitter.split(source, proceedings, tempDir);

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

    @Test
    void splitsOnlyProceedings() {
        assertTrue(PdfOutlineSplitter.isProceedings(StandardEntryType.Proceedings));
        assertTrue(PdfOutlineSplitter.isProceedings(StandardEntryType.MvProceedings));
        assertFalse(PdfOutlineSplitter.isProceedings(StandardEntryType.Book));
        assertFalse(PdfOutlineSplitter.isProceedings(StandardEntryType.Collection));
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

        List<BibEntry> papers = PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir);

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

        List<BibEntry> papers = PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir);

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

        List<BibEntry> papers = PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir);

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

        List<BibEntry> papers = PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir);

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

        assertEquals(List.of(), PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir));
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

        assertEquals(List.of(), PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir));
    }

    @Test
    void returnsNothingWhenThePdfHasNoOutline() throws IOException {
        Path source = tempDir.resolve("plain.pdf");
        try (PDDocument document = new PDDocument()) {
            addPages(document, 3);
            document.save(source.toFile());
        }

        assertEquals(List.of(), PdfOutlineSplitter.split(source, new BibEntry(StandardEntryType.Proceedings), tempDir));
        assertEquals(3, pageCount(source));
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

    /// A bookmark to a page that starts with a paper title set larger than the body text
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
