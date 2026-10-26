package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.isbntobibtex.IsbnFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class PdfBookMetadataMergeTest {

    private static final String ISBN = "9789660800007";

    @TempDir
    Path tempDir;

    @Test
    void retainsLocalBookWhenLookupReturnsNoResult() throws IOException, ParseException, FetcherException {
        assertMergedBook(new ParserResult(List.of(localBookWithIsbn())),
                (fetcher, _) -> when(fetcher.performSearchById(ISBN)).thenReturn(Optional.empty()),
                localBookWithIsbn());
    }

    @Test
    void retainsLocalFieldsWhenLookupReturnsAnEmptyEntry() throws IOException, ParseException, FetcherException {
        assertMergedBook(new ParserResult(List.of(localBookWithIsbn())),
                (fetcher, _) -> when(fetcher.performSearchById(ISBN)).thenReturn(Optional.of(new BibEntry())),
                localBookWithIsbn());
    }

    @Test
    void retainsLocalBookAndParserWarningWhenLookupFails() throws IOException, ParseException, FetcherException {
        ParserResult extracted = new ParserResult(List.of(localBookWithIsbn()));
        extracted.addWarning("Local extraction warning");

        assertMergedBook(extracted,
                (fetcher, _) -> when(fetcher.performSearchById(ISBN)).thenThrow(new FetcherException("Controlled lookup failure")),
                localBookWithIsbn());
    }

    @Test
    void fillsIncompleteLookupWithLocalFields() throws IOException, ParseException, FetcherException {
        BibEntry fetched = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Title from ISBN lookup")
                .withField(StandardField.URL, "https://example.org/book");
        BibEntry expected = localBookWithIsbn()
                .withField(StandardField.TITLE, "Title from ISBN lookup")
                .withField(StandardField.URL, "https://example.org/book");

        assertMergedBook(new ParserResult(List.of(localBookWithIsbn())),
                (fetcher, _) -> when(fetcher.performSearchById(ISBN)).thenReturn(Optional.of(fetched)), expected);
    }

    @Test
    void prefersSuccessfulLookupForConflictingFields() throws IOException, ParseException, FetcherException {
        BibEntry fetched = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Title from ISBN lookup")
                .withField(StandardField.AUTHOR, "Коваль, О. П. and Ткач, Н. І.")
                .withField(StandardField.PUBLISHER, "Fetched publisher")
                .withField(StandardField.YEAR, "2012")
                .withField(StandardField.ISBN, ISBN);
        BibEntry expected = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Title from ISBN lookup")
                .withField(StandardField.AUTHOR, "Коваль, О. П. and Ткач, Н. І.")
                .withField(StandardField.PUBLISHER, "Fetched publisher")
                .withField(StandardField.YEAR, "2012")
                .withField(StandardField.ISBN, ISBN)
                .withField(StandardField.EDITION, "2");
        ParserResult extracted = new ParserResult(List.of(localBookWithIsbn()));
        extracted.addWarning("Local extraction warning");

        assertMergedBook(extracted,
                (fetcher, _) -> when(fetcher.performSearchById(ISBN)).thenReturn(Optional.of(fetched)), expected);
    }

    @Test
    void retainsLocalBookWithoutLookupWhenNoIsbnIsSelected() throws IOException, ParseException, FetcherException {
        assertMergedBook(new ParserResult(List.of(localBook())), (_, _) -> { }, localBook());
    }

    @Test
    void retainsAmbiguousBookAndWarningWithoutLookup() throws IOException, ParseException, FetcherException {
        ParserResult extracted = new ParserResult(List.of(localBook()));
        extracted.addWarning(Localization.lang("More than one ISBN found"));

        assertMergedBook(extracted, (_, _) -> { }, localBook());
    }

    private BibEntry localBook() {
        return new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Збірник задач з фізики для 8 класу")
                .withField(StandardField.AUTHOR, "О. П. Коваль and Н. І. Ткач")
                .withField(StandardField.PUBLISHER, "Ранок")
                .withField(StandardField.YEAR, "2010")
                .withField(StandardField.EDITION, "2");
    }

    private BibEntry localBookWithIsbn() {
        return localBook().withField(StandardField.ISBN, ISBN);
    }

    /// Supply the content result independently of PR2 entry assembly and intercept every identifier fetcher.
    private void assertMergedBook(ParserResult extracted, MockedConstruction.MockInitializer<IsbnFetcher> isbnSetup,
                                  BibEntry expected) throws IOException, ParseException, FetcherException {
        ImportFormatPreferences preferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.fieldPreferences().getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
        when(preferences.grobidPreferences().isGrobidEnabled()).thenReturn(false);
        Path file = tempDir.resolve("book.pdf");
        BibEntry localEntry = extracted.getDatabase().getEntries().getFirst();
        BookExtraction book = new BookExtraction(Optional.of(localEntry),
                localEntry.getField(StandardField.ISBN)
                          .flatMap(org.jabref.model.entry.identifier.ISBN::parse)
                          .stream().toList());

        try (MockedConstruction<PdfContentImporter> contentImporters = mockConstruction(PdfContentImporter.class,
                (content, _) -> {
                    when(content.extractBook(any(PDDocument.class))).thenReturn(Optional.of(book));
                    when(content.importBook(book)).thenReturn(extracted);
                });
             MockedConstruction<DoiFetcher> doiFetchers = mockConstruction(DoiFetcher.class);
             MockedConstruction<ArXivFetcher> arXivFetchers = mockConstruction(ArXivFetcher.class);
             MockedConstruction<IsbnFetcher> isbnFetchers = mockConstruction(IsbnFetcher.class, isbnSetup);
             PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            PdfMergeMetadataImporter importer = new PdfMergeMetadataImporter(preferences);

            ParserResult result = importer.importDatabase(file, document);

            expected.addFile(new LinkedFile("", file, StandardFileType.PDF.getName()));
            assertEquals(List.of(expected), result.getDatabase().getEntries());
            assertEquals(extracted.warnings(), result.warnings());
            verify(contentImporters.constructed().getFirst()).extractBook(document);
            verify(contentImporters.constructed().getFirst()).importBook(book);
            verifyNoInteractions(doiFetchers.constructed().getFirst(), arXivFetchers.constructed().getFirst());
            IsbnFetcher isbnFetcher = isbnFetchers.constructed().getFirst();
            if (extracted.getDatabase().getEntries().getFirst().getField(StandardField.ISBN).isPresent()) {
                verify(isbnFetcher).performSearchById(ISBN);
                verifyNoMoreInteractions(isbnFetcher);
            } else {
                verifyNoInteractions(isbnFetcher);
            }
        }
    }
}
