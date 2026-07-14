package org.jabref.logic.directorylibrary;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PdfEntryFactoryTest {

    private final CrossRef crossRef = mock(CrossRef.class);
    private final DoiFetcher doiFetcher = mock(DoiFetcher.class);
    private final PdfEntryFactory factory = new PdfEntryFactory(
            offlineImportFormatPreferences(),
            mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS),
            DirectoryLibraryScannerTest.authYearPatternPreferences(),
            crossRef,
            doiFetcher);

    private static ImportFormatPreferences offlineImportFormatPreferences() {
        GrobidPreferences noGrobid = mock(GrobidPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(noGrobid.isGrobidEnabled()).thenReturn(false);
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.fieldPreferences().getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
        when(importFormatPreferences.grobidPreferences()).thenReturn(noGrobid);
        return importFormatPreferences;
    }

    @Test
    void missingDoiIsLookedUpAndItsMetadataFillsOnlyEmptyFields() throws FetcherException, URISyntaxException {
        when(crossRef.findIdentifier(any())).thenReturn(Optional.of(DOI.parse("10.1000/demo").orElseThrow()));
        BibEntry fetched = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Fetched Title Must Not Win")
                .withField(StandardField.JOURNAL, "Fetched Journal");
        when(doiFetcher.performSearchById("10.1000/demo")).thenReturn(Optional.of(fetched));
        Path pdf = Path.of(getClass().getResource("/pdfs/PdfContentImporter/Kriha2018.pdf").toURI());

        BibEntry extracted = factory.extractMetadata(pdf, new BibDatabaseContext()).orElseThrow();

        // The PDF-extracted title has priority; DOI metadata only fills gaps
        assertEquals(Optional.of("On How We Can Teach – Exploring New Ways in Professional Software Development for Students"),
                extracted.getField(StandardField.TITLE));
        assertEquals(Optional.of("Fetched Journal"), extracted.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("10.1000/demo"), extracted.getField(StandardField.DOI));
    }

    @Test
    void generatesCitationKeyFromPattern() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Doe, John")
                .withField(StandardField.YEAR, "2016");
        databaseContext.getDatabase().insertEntry(entry);

        factory.generateCitationKeyIfMissing(entry, databaseContext);

        assertEquals(Optional.of("Doe2016"), entry.getCitationKey());
    }

    @Test
    void generatedCitationKeysAreUniqueWithinTheLibrary() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        BibEntry first = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Doe, John")
                .withField(StandardField.YEAR, "2016")
                .withCitationKey("Doe2016");
        BibEntry second = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Doe, John")
                .withField(StandardField.YEAR, "2016");
        databaseContext.getDatabase().insertEntries(List.of(first, second));

        factory.generateCitationKeyIfMissing(second, databaseContext);

        assertEquals(Optional.of("Doe2016a"), second.getCitationKey());
    }

    @Test
    void keepsExistingCitationKey() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Doe, John")
                .withField(StandardField.YEAR, "2016")
                .withCitationKey("customKey");
        databaseContext.getDatabase().insertEntry(entry);

        factory.generateCitationKeyIfMissing(entry, databaseContext);

        assertEquals(Optional.of("customKey"), entry.getCitationKey());
    }
}
