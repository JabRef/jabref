package org.jabref.logic.externalfiles;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.externalfiles.PdfLocationResolver.PageSpan;
import org.jabref.logic.externalfiles.PdfLocationResolver.PdfLocation;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@NullMarked
class PdfLocationResolverTest {

    private static final LinkedFile PROCEEDINGS_PDF = new LinkedFile("Proceedings", "proc.pdf", StandardFileType.PDF);
    private static final LinkedFile PAPER_PDF = new LinkedFile("Paper", "paper.pdf", StandardFileType.PDF);

    // [utest->req~logic.externalfiles.pdf-location-resolver~1]
    @Test
    void usesTheEntryOwnPdfAndFirstPage() {
        BibEntry paper = paper("186--206").withFiles(List.of(PAPER_PDF));

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(paper)).orElseThrow();

        assertEquals(PAPER_PDF, location.file());
        assertEquals(Optional.of(186), location.page());
        assertSame(paper, location.fileOwner());
    }

    // [utest->req~logic.externalfiles.pdf-location-resolver~1]
    @ParameterizedTest
    @CsvSource(textBlock = """
            45, 100, 45
            45, 8,   1
            8,  8,   8
            """)
    void opensTheFirstPageWhenThePagePassesTheEndOfThePdf(int page, int pageCount, int expectedPage) {
        PdfLocation location = new PdfLocation(PAPER_PDF, Optional.of(page), paper("45--52"));

        assertEquals(expectedPage, location.pageWithin(pageCount));
    }

    @Test
    void opensTheFirstPageWithoutAPage() {
        assertEquals(1, new PdfLocation(PAPER_PDF, Optional.empty(), paper("")).pageWithin(8));
    }

    @Test
    void addsTheOffsetStoredOnTheFileOwner() {
        BibEntry paper = paper("186--206")
                .withFiles(List.of(PAPER_PDF))
                .withField(PdfLocationResolver.PAGE_OFFSET, "14");

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(paper)).orElseThrow();

        assertEquals(Optional.of(200), location.page());
        assertSame(paper, location.fileOwner());
    }

    // [utest->req~logic.externalfiles.pdf-location-resolver~1]
    @Test
    void usesTheCrossReferencedPdfAndItsOffset() {
        BibEntry proceedings = proceedings().withField(PdfLocationResolver.PAGE_OFFSET, "14");
        BibEntry paper = paper("186--206");
        BibDatabase database = databaseOf(proceedings, paper);

        PdfLocation location = PdfLocationResolver.resolve(paper, database).orElseThrow();

        assertEquals(PROCEEDINGS_PDF, location.file());
        assertEquals(Optional.of(200), location.page());
        assertSame(proceedings, location.fileOwner());
    }

    @Test
    void missingOffsetLeavesThePrintedPageUnchanged() {
        BibEntry proceedings = proceedings();
        BibEntry paper = paper("186--206");

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(proceedings, paper)).orElseThrow();

        assertEquals(Optional.of(186), location.page());
    }

    // [utest->req~logic.externalfiles.pdf-location-resolver~1]
    @Test
    void usesTheCrossReferencedOffsetWhenTheEntryHasTheSamePdf() {
        BibEntry proceedings = proceedings().withField(PdfLocationResolver.PAGE_OFFSET, "14");
        BibEntry paper = paper("10--12").withFiles(List.of(PROCEEDINGS_PDF));

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(proceedings, paper)).orElseThrow();

        assertEquals(Optional.of(24), location.page());
        assertSame(paper, location.fileOwner());
    }

    @Test
    void ignoresTheCrossReferencedOffsetForAnotherPdf() {
        BibEntry proceedings = proceedings().withField(PdfLocationResolver.PAGE_OFFSET, "14");
        BibEntry paper = paper("10--12").withFiles(List.of(PAPER_PDF));

        assertEquals(Optional.of(10), PdfLocationResolver.resolve(paper, databaseOf(proceedings, paper)).flatMap(PdfLocation::page));
    }

    @Test
    void ignoresPageOffsetOnThePaperWhenThePdfBelongsToTheProceedings() {
        BibEntry proceedings = proceedings().withField(PdfLocationResolver.PAGE_OFFSET, "14");
        BibEntry paper = paper("10--12").withField(PdfLocationResolver.PAGE_OFFSET, "5");

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(proceedings, paper)).orElseThrow();

        assertEquals(Optional.of(24), location.page());
        assertSame(proceedings, location.fileOwner());
    }

    @Test
    void prefersTheEntryOwnPdfOverTheCrossReferencedOne() {
        BibEntry proceedings = proceedings().withField(PdfLocationResolver.PAGE_OFFSET, "14");
        BibEntry paper = paper("10--12").withFiles(List.of(PAPER_PDF));

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(proceedings, paper)).orElseThrow();

        assertEquals(PAPER_PDF, location.file());
        assertEquals(Optional.of(10), location.page());
        assertSame(paper, location.fileOwner());
    }

    @Test
    void skipsNonPdfFilesAttachedToTheEntry() {
        LinkedFile notes = new LinkedFile("Notes", "notes.txt", StandardFileType.TXT);
        BibEntry proceedings = proceedings();
        BibEntry paper = paper("15--20").withFiles(List.of(notes));

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(proceedings, paper)).orElseThrow();

        assertEquals(PROCEEDINGS_PDF, location.file());
        assertEquals(Optional.of(15), location.page());
    }

    @Test
    void treatsAPdfExtensionAsPdfWhenTheFileTypeIsEmpty() {
        LinkedFile untyped = new LinkedFile("", "chapter.pdf", "");
        BibEntry paper = paper("4--8").withFiles(List.of(untyped));

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(paper)).orElseThrow();

        assertEquals(untyped, location.file());
        assertEquals(Optional.of(4), location.page());
    }

    @Test
    void acceptsAnOnlinePdfByItsFileType() {
        LinkedFile online = new LinkedFile("", "https://example.org/proc.pdf", StandardFileType.PDF.getName());
        BibEntry proceedings = new BibEntry(StandardEntryType.Proceedings)
                .withCitationKey("Proc2024")
                .withFiles(List.of(online));
        BibEntry paper = paper("3--9");

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(proceedings, paper)).orElseThrow();

        assertEquals(online, location.file());
        assertSame(proceedings, location.fileOwner());
    }

    @Test
    void returnsTheFileWithoutAPageWhenPagesAreMissing() {
        BibEntry proceedings = proceedings();
        BibEntry paper = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("Smith2024")
                .withField(StandardField.CROSSREF, "Proc2024");

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(proceedings, paper)).orElseThrow();

        assertEquals(PROCEEDINGS_PDF, location.file());
        assertEquals(Optional.empty(), location.page());
    }

    @Test
    void returnsTheFileWithoutAPageWhenPagesStartWithARomanNumeral() {
        BibEntry proceedings = proceedings();
        BibEntry paper = paper("iv--xx");

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(proceedings, paper)).orElseThrow();

        assertEquals(PROCEEDINGS_PDF, location.file());
        assertEquals(Optional.empty(), location.page());
    }

    @Test
    void doesNotSkipALeadingRomanNumeralToALaterArabicPage() {
        BibEntry paper = paper("iv, 1--10").withFiles(List.of(PAPER_PDF));

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(paper)).orElseThrow();

        assertEquals(Optional.empty(), location.page());
    }

    @Test
    void returnsTheFileWithoutAPageWhenTheOffsetMovesBeforeTheFirstPdfPage() {
        BibEntry proceedings = proceedings().withField(PdfLocationResolver.PAGE_OFFSET, "-5");
        BibEntry paper = paper("2--4");

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(proceedings, paper)).orElseThrow();

        assertEquals(PROCEEDINGS_PDF, location.file());
        assertEquals(Optional.empty(), location.page());
    }

    @Test
    void ignoresANonNumericOffset() {
        BibEntry proceedings = proceedings().withField(PdfLocationResolver.PAGE_OFFSET, "xiv");
        BibEntry paper = paper("12--18");

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(proceedings, paper)).orElseThrow();

        assertEquals(Optional.of(12), location.page());
    }

    @Test
    void appliesANegativeOffset() {
        BibEntry proceedings = proceedings().withField(PdfLocationResolver.PAGE_OFFSET, "-3");
        BibEntry paper = paper("10--20");

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(proceedings, paper)).orElseThrow();

        assertEquals(Optional.of(7), location.page());
    }

    @Test
    void doesNotFollowXref() {
        BibEntry proceedings = proceedings();
        BibEntry paper = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("Smith2024")
                .withField(StandardField.XREF, "Proc2024")
                .withField(StandardField.PAGES, "10--12");

        assertEquals(Optional.empty(), PdfLocationResolver.resolve(paper, databaseOf(proceedings, paper)));
    }

    @Test
    void doesNotWalkPastTheFirstCrossref() {
        BibEntry book = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Book2024")
                .withFiles(List.of(PROCEEDINGS_PDF));
        BibEntry chapter = new BibEntry(StandardEntryType.InBook)
                .withCitationKey("Chapter2024")
                .withField(StandardField.CROSSREF, "Book2024");
        BibEntry paper = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("Smith2024")
                .withField(StandardField.CROSSREF, "Chapter2024")
                .withField(StandardField.PAGES, "10--12");

        assertEquals(Optional.empty(), PdfLocationResolver.resolve(paper, databaseOf(book, chapter, paper)));
    }

    @Test
    void returnsEmptyWhenNoPdfIsAttached() {
        BibEntry proceedings = new BibEntry(StandardEntryType.Proceedings).withCitationKey("Proc2024");
        BibEntry paper = paper("10--12");

        assertEquals(Optional.empty(), PdfLocationResolver.resolve(paper, databaseOf(proceedings, paper)));
    }

    @Test
    void returnsEmptyWhenTheCrossrefTargetIsMissing() {
        BibEntry paper = paper("10--12");

        assertEquals(Optional.empty(), PdfLocationResolver.resolve(paper, databaseOf(paper)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            186--206|186
            186-206|186
            186 -- 206|186
            43+|43
            7,41,73--97|7
            S186--S190|186
            """)
    void readsTheFirstArabicPage(String pages, int expectedPage) {
        BibEntry paper = paper(pages).withFiles(List.of(PAPER_PDF));

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(paper)).orElseThrow();

        assertEquals(Optional.of(expectedPage), location.page());
    }

    @Test
    void pageOffsetOwnerIsTheEntryOnceItHasAnOffset() {
        BibEntry proceedings = proceedings().withField(PdfLocationResolver.PAGE_OFFSET, "14");
        BibEntry paper = paper("186--206").withField(PdfLocationResolver.PAGE_OFFSET, "0");

        assertSame(paper, PdfLocationResolver.pageOffsetOwner(paper, new BibDatabase(List.of(proceedings, paper))));
    }

    @Test
    void pageOffsetOwnerIsTheCrossReferencedEntryWhenTheEntryHasNoOffset() {
        BibEntry proceedings = proceedings().withField(PdfLocationResolver.PAGE_OFFSET, "14");
        BibEntry paper = paper("186--206");

        assertSame(proceedings, PdfLocationResolver.pageOffsetOwner(paper, new BibDatabase(List.of(proceedings, paper))));
    }

    @Test
    void pageSpanUsesTheFirstRangeAndTheFileOwnersOffset() {
        BibEntry paper = paper("186--206");
        BibEntry proceedings = proceedings().withField(PdfLocationResolver.PAGE_OFFSET, "14");

        assertEquals(Optional.of(new PageSpan(200, 220)), PdfLocationResolver.pdfPageSpan(paper, proceedings));
    }

    @Test
    void pageSpanIsEmptyWhenPagesContinueWithoutAnEnd() {
        assertEquals(Optional.empty(), PdfLocationResolver.pdfPageSpan(paper("43+"), proceedings()));
    }

    @Test
    void readsAnEnDashPageRange() {
        BibEntry paper = paper("13905\u201313911").withFiles(List.of(PAPER_PDF));

        PdfLocation location = PdfLocationResolver.resolve(paper, databaseOf(paper)).orElseThrow();

        assertEquals(Optional.of(13905), location.page());
    }

    private static BibEntry proceedings() {
        return new BibEntry(StandardEntryType.Proceedings)
                .withCitationKey("Proc2024")
                .withFiles(List.of(PROCEEDINGS_PDF));
    }

    private static BibEntry paper(String pages) {
        return new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("Smith2024")
                .withField(StandardField.CROSSREF, "Proc2024")
                .withField(StandardField.PAGES, pages);
    }

    private static BibDatabase databaseOf(BibEntry... entries) {
        BibDatabase database = new BibDatabase();
        database.insertEntries(entries);
        return database;
    }
}
