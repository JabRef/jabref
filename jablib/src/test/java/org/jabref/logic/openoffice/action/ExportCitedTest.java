package org.jabref.logic.openoffice.action;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class ExportCitedTest {

    @Test
    void generateDatabaseFromCitationKeysIncludesCrossrefOnce() {
        BibEntry parentEntry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Parent2024")
                .withField(StandardField.TITLE, "Parent title");
        BibEntry childEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Child2024")
                .withField(StandardField.TITLE, "Child title")
                .withField(StandardField.CROSSREF, "Parent2024");
        BibDatabase database = new BibDatabase(List.of(childEntry, parentEntry));

        ExportCited.GenerateDatabaseResult result = ExportCited.generateDatabaseFromCitationKeys(
                List.of("Child2024", "Child2024"),
                List.of(database));

        assertEquals(List.of(), result.unresolvedKeys);
        assertEquals(2, result.newDatabase.getEntryCount());
        assertTrue(result.newDatabase.getEntryByCitationKey("Child2024").isPresent());
        assertTrue(result.newDatabase.getEntryByCitationKey("Parent2024").isPresent());
        assertNotSame(childEntry, result.newDatabase.getEntryByCitationKey("Child2024").orElseThrow());
    }

    @Test
    void generateDatabaseFromCitationKeysReportsUnresolvedKeysOnce() {
        BibEntry knownEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Known2024");
        BibDatabase database = new BibDatabase(List.of(knownEntry));

        ExportCited.GenerateDatabaseResult result = ExportCited.generateDatabaseFromCitationKeys(
                List.of("Missing2024", "Missing2024"),
                List.of(database));

        assertEquals(List.of("Missing2024"), result.unresolvedKeys);
        assertEquals(0, result.newDatabase.getEntryCount());
    }

    @Test
    void generateDatabaseFromIdentifiersFallsBackToEntryId() {
        BibEntry entryWithoutCitationKey = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Entry without citation key");
        BibDatabase database = new BibDatabase(List.of(entryWithoutCitationKey));

        ExportCited.GenerateDatabaseResult result = ExportCited.generateDatabaseFromIdentifiers(
                List.of(entryWithoutCitationKey.getId()),
                List.of(database));

        assertEquals(List.of(), result.unresolvedKeys);
        assertEquals(1, result.newDatabase.getEntryCount());
        assertEquals(
                "Entry without citation key",
                result.newDatabase.getEntries().getFirst().getField(StandardField.TITLE).orElseThrow());
    }
}
