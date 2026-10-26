package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class PdfBookIsbnResolutionTest {

    private static final String MATCHING_ISBN = "9780596520687";
    private static final String OTHER_ISBN = "9780134685991";
    private static final String AMBIGUOUS_ISBNS = "ISBN " + MATCHING_ISBN + "\nISBN " + OTHER_ISBN;

    @TempDir
    Path tempDir;

    private IdBasedFetcher isbnFetcher;
    private PdfMergeMetadataImporter importer;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences preferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.fieldPreferences().getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
        when(preferences.grobidPreferences().isGrobidEnabled()).thenReturn(false);
        isbnFetcher = mock(IdBasedFetcher.class);
        importer = new PdfMergeMetadataImporter(preferences, isbnFetcher);
    }

    // [utest->req~import.pdf.book-isbn-resolution~1]
    @Test
    void mergesOnlySelectedMetadataWithoutRefetchingOrWarning() throws IOException, ParseException, FetcherException {
        BibEntry matching = localBook()
                .withField(StandardField.ISBN, MATCHING_ISBN)
                .withField(StandardField.ABSTRACT, "Selected lookup metadata");
        when(isbnFetcher.performSearchById(MATCHING_ISBN)).thenReturn(Optional.of(matching));
        when(isbnFetcher.performSearchById(OTHER_ISBN)).thenReturn(Optional.of(differentYearBook()));

        ParserResult result = importBook(AMBIGUOUS_ISBNS);

        assertEquals(List.of(withPdf(new BibEntry(matching))), result.getDatabase().getEntries());
        assertEquals(List.of(), result.warnings());
        verify(isbnFetcher).performSearchById(MATCHING_ISBN);
        verify(isbnFetcher).performSearchById(OTHER_ISBN);
        verifyNoMoreInteractions(isbnFetcher);
    }

    @Test
    void retainsLocalMetadataAndWarningWhenAnotherCandidateIsUnknown() throws IOException, ParseException, FetcherException {
        when(isbnFetcher.performSearchById(MATCHING_ISBN))
                .thenReturn(Optional.of(localBook().withField(StandardField.ISBN, MATCHING_ISBN)));
        when(isbnFetcher.performSearchById(OTHER_ISBN)).thenReturn(Optional.empty());

        assertUnresolved(importBook(AMBIGUOUS_ISBNS));
        verify(isbnFetcher).performSearchById(OTHER_ISBN);
    }

    @Test
    void retainsLocalMetadataAndWarningWhenLookupFails() throws IOException, ParseException, FetcherException {
        when(isbnFetcher.performSearchById(MATCHING_ISBN))
                .thenReturn(Optional.of(localBook().withField(StandardField.ISBN, MATCHING_ISBN)));
        when(isbnFetcher.performSearchById(OTHER_ISBN)).thenThrow(new FetcherException("Lookup failed", new IOException()));

        assertUnresolved(importBook(AMBIGUOUS_ISBNS));
    }

    @Test
    void retainsWarningWhenBothCandidatesMatch() throws IOException, ParseException, FetcherException {
        when(isbnFetcher.performSearchById(MATCHING_ISBN))
                .thenReturn(Optional.of(localBook().withField(StandardField.ISBN, MATCHING_ISBN)));
        when(isbnFetcher.performSearchById(OTHER_ISBN))
                .thenReturn(Optional.of(localBook().withField(StandardField.ISBN, OTHER_ISBN)));

        assertUnresolved(importBook(AMBIGUOUS_ISBNS));
    }

    @Test
    void normalizesEquivalentCandidatesBeforeFetching() throws IOException, ParseException, FetcherException {
        when(isbnFetcher.performSearchById(MATCHING_ISBN))
                .thenReturn(Optional.of(localBook().withField(StandardField.ISBN, MATCHING_ISBN)));
        when(isbnFetcher.performSearchById(OTHER_ISBN)).thenReturn(Optional.of(differentYearBook()));

        ParserResult result = importBook(AMBIGUOUS_ISBNS + "\nISBN 0-596-52068-9");

        assertEquals(List.of(withPdf(localBook().withField(StandardField.ISBN, MATCHING_ISBN))), result.getDatabase().getEntries());
        assertEquals(List.of(), result.warnings());
        verify(isbnFetcher).performSearchById(MATCHING_ISBN);
        verify(isbnFetcher).performSearchById(OTHER_ISBN);
        verifyNoMoreInteractions(isbnFetcher);
    }

    @Test
    void preservesSingleIsbnWhenLookupFindsNothing() throws IOException, ParseException, FetcherException {
        when(isbnFetcher.performSearchById(MATCHING_ISBN)).thenReturn(Optional.empty());

        ParserResult result = importBook("ISBN " + MATCHING_ISBN);

        assertEquals(List.of(withPdf(localBook().withField(StandardField.ISBN, MATCHING_ISBN))), result.getDatabase().getEntries());
        assertEquals(List.of(), result.warnings());
        verify(isbnFetcher).performSearchById(MATCHING_ISBN);
    }

    @Test
    void fillsMissingLookupFieldsFromLocalMetadata() throws IOException, ParseException, FetcherException {
        BibEntry partial = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.ISBN, MATCHING_ISBN)
                .withField(StandardField.ABSTRACT, "Available lookup metadata");
        when(isbnFetcher.performSearchById(MATCHING_ISBN)).thenReturn(Optional.of(partial));

        ParserResult result = importBook("ISBN " + MATCHING_ISBN);

        BibEntry expected = localBook()
                .withField(StandardField.ISBN, MATCHING_ISBN)
                .withField(StandardField.ABSTRACT, "Available lookup metadata");
        assertEquals(List.of(withPdf(expected)), result.getDatabase().getEntries());
        assertEquals(List.of(), result.warnings());
    }

    @Test
    void importsLocalMetadataWithoutLookingUpAbsentIsbns() throws IOException, ParseException {
        ParserResult result = importBook("");

        assertEquals(List.of(withPdf(localBook())), result.getDatabase().getEntries());
        assertEquals(List.of(), result.warnings());
        verifyNoInteractions(isbnFetcher);
    }

    @Test
    void skipsResolutionWhenLocalContributorsAreMissing() throws IOException, ParseException {
        String pageText = citationPage(AMBIGUOUS_ISBNS).replace("О. П. Коваль, Н. І. Ткач", "");
        ParserResult result = importPage(pageText);
        BibEntry expected = localBook();
        expected.clearField(StandardField.AUTHOR);

        assertEquals(List.of(withPdf(expected)), result.getDatabase().getEntries());
        assertEquals(List.of(Localization.lang("More than one ISBN found")), result.warnings());
        verifyNoInteractions(isbnFetcher);
    }

    @Test
    void doesNotReuseLookupResultsOrWarningsAcrossDocuments() throws IOException, ParseException, FetcherException {
        when(isbnFetcher.performSearchById(MATCHING_ISBN))
                .thenReturn(Optional.of(localBook().withField(StandardField.ISBN, MATCHING_ISBN)));
        when(isbnFetcher.performSearchById(OTHER_ISBN))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(differentYearBook()));

        assertUnresolved(importBook(AMBIGUOUS_ISBNS));
        ParserResult nextResult = importBook(AMBIGUOUS_ISBNS);

        assertEquals(List.of(withPdf(localBook().withField(StandardField.ISBN, MATCHING_ISBN))), nextResult.getDatabase().getEntries());
        assertEquals(List.of(), nextResult.warnings());
        verify(isbnFetcher, times(2)).performSearchById(MATCHING_ISBN);
        verify(isbnFetcher, times(2)).performSearchById(OTHER_ISBN);
    }

    private void assertUnresolved(ParserResult result) {
        assertEquals(List.of(withPdf(localBook())), result.getDatabase().getEntries());
        assertEquals(List.of(Localization.lang("More than one ISBN found")), result.warnings());
    }

    private ParserResult importBook(String isbns) throws IOException, ParseException {
        return importPage(citationPage(isbns));
    }

    private ParserResult importPage(String pageText) throws IOException, ParseException {
        try (PDDocument document = new PDDocument();
             MockedStatic<PdfPageScanner> scanner = mockStatic(PdfPageScanner.class)) {
            document.addPage(new PDPage());
            scanner.when(() -> PdfPageScanner.scanPages(document)).thenReturn(List.of(pageText));

            ParserResult result = importer.importDatabase(tempDir.resolve("book.pdf"), document);
            scanner.verify(() -> PdfPageScanner.scanPages(document));
            return result;
        }
    }

    private String citationPage(String isbns) {
        return """
                УДК 53(075.3)
                Коваль О. П.
                К56 Збірник задач з фізики для 8 класу / О. П. Коваль, Н. І. Ткач. — Харків : Ранок, 2010. — 176 с.
                %s
                © Коваль О. П., 2008
                """.formatted(isbns);
    }

    private BibEntry localBook() {
        return new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Збірник задач з фізики для 8 класу")
                .withField(StandardField.AUTHOR, "О. П. Коваль and Н. І. Ткач")
                .withField(StandardField.PUBLISHER, "Ранок")
                .withField(StandardField.YEAR, "2010");
    }

    private BibEntry differentYearBook() {
        return localBook()
                .withField(StandardField.ISBN, OTHER_ISBN)
                .withField(StandardField.YEAR, "1999")
                .withField(StandardField.NOTE, "Rejected lookup metadata");
    }

    private BibEntry withPdf(BibEntry entry) {
        entry.addFile(new LinkedFile("", tempDir.resolve("book.pdf"), StandardFileType.PDF.getName()));
        return entry;
    }
}
