package org.jabref.logic.directorylibrary;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class PdfEntryFactoryTest {

    private final PdfEntryFactory factory = new PdfEntryFactory(
            mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS),
            mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS),
            DirectoryLibraryScannerTest.authYearPatternPreferences());

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
