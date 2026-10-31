package org.jabref.logic.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.externalfiles.PdfLocationResolver;
import org.jabref.logic.externalfiles.PdfLocationResolver.PageSpan;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class PdfPaperExtractorTest {

    @TempDir
    private Path tempDir;

    // [utest->req~logic.pdf.extract-paper-page-range~1]
    @Test
    void copiesTheEntryPagesAndLeavesTheOriginalIntact() throws IOException {
        Path source = blankPdf("proceedings.pdf", 5);
        BibEntry paper = new BibEntry(StandardEntryType.InProceedings).withField(StandardField.PAGES, "2--3");

        Optional<Path> extracted = extractPaper(source, paper, paper);

        assertEquals(2, pageCount(extracted.orElseThrow()));
        assertEquals(5, pageCount(source));
    }

    @Test
    void shiftsTheRangeByTheEntryPageOffset() throws IOException {
        Path source = blankPdf("proceedings.pdf", 20);
        BibEntry paper = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.PAGES, "2--3")
                .withField(PdfLocationResolver.PAGE_OFFSET, "14");

        Optional<Path> extracted = extractPaper(source, paper, paper);

        assertEquals(2, pageCount(extracted.orElseThrow()));
        assertEquals("proceedings-p16-17.pdf", extracted.orElseThrow().getFileName().toString());
    }

    // [utest->req~logic.pdf.extract-paper-page-range~1]
    @Test
    void shiftsTheRangeByTheFileOwnersPageOffset() throws IOException {
        Path source = blankPdf("proceedings.pdf", 20);
        BibEntry volume = new BibEntry(StandardEntryType.Proceedings)
                .withCitationKey("volume")
                .withField(PdfLocationResolver.PAGE_OFFSET, "14");
        BibEntry paper = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.PAGES, "2--3")
                .withField(StandardField.CROSSREF, "volume");

        Optional<Path> extracted = extractPaper(source, paper, volume);

        assertEquals("proceedings-p16-17.pdf", extracted.orElseThrow().getFileName().toString());
    }

    @Test
    void doesNotCopyWhenThePdfIsAlreadyJustThePaper() throws IOException {
        Path source = blankPdf("paper.pdf", 4);
        BibEntry paper = new BibEntry(StandardEntryType.InProceedings).withField(StandardField.PAGES, "1--4");

        assertEquals(Optional.empty(), extractPaper(source, paper, paper));
        assertEquals(4, pageCount(source));
    }

    @Test
    void doesNotCopyWhenThePagesFieldHasNoEnd() throws IOException {
        Path source = blankPdf("proceedings.pdf", 5);
        BibEntry paper = new BibEntry(StandardEntryType.InProceedings).withField(StandardField.PAGES, "43+");

        assertEquals(Optional.empty(), extractPaper(source, paper, paper));
    }

    private Optional<Path> extractPaper(Path source, BibEntry entry, BibEntry fileOwner) throws IOException {
        Optional<PageSpan> pages = PdfPaperExtractor.pagesToExtract(source, entry, fileOwner);
        if (pages.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(PdfPaperExtractor.extractPages(source, pages.orElseThrow(), tempDir));
    }

    private Path blankPdf(String name, int pages) throws IOException {
        Path path = tempDir.resolve(name);
        try (PDDocument document = new PDDocument()) {
            for (int page = 0; page < pages; page++) {
                document.addPage(new PDPage());
            }
            document.save(path.toFile());
        }
        return path;
    }

    private static int pageCount(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            return document.getNumberOfPages();
        }
    }
}
