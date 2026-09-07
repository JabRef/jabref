package org.jabref.logic.shared;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineChangesTest {

    @TempDir
    Path directory;

    private DBMSConnectionProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setHost("localhost")
                .setPort(5432)
                .setDatabase("bib")
                .setUser("alice")
                .setPassword("secret")
                .createDBMSConnectionProperties();
    }

    private static BibEntry sharedEntry(int sharedId, int version) {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Title " + sharedId);
        entry.getSharedBibEntryData().setSharedId(sharedId);
        entry.getSharedBibEntryData().setVersion(version);
        return entry;
    }

    @Test
    void fileHoldsEveryKindOfRecordAsJson() throws Exception {
        OfflineChanges changes = OfflineChanges.load(directory, properties);
        changes.recordChange(sharedEntry(1, 3).withField(StandardField.YEAR, "2026"));
        BibEntry newEntry = new BibEntry(StandardEntryType.Book).withField(StandardField.TITLE, "New");
        newEntry.setId("local-id");
        changes.recordInsert(List.of(newEntry));
        changes.recordRemoval(List.of(sharedEntry(2, 5)));
        changes.recordMetaData(Map.of("databaseType", "bibtex"));

        String expected = """
                {
                  "changedEntries": {
                    "1": {
                      "baseVersion": 3,
                      "entryType": "article",
                      "fields": {
                        "year": "2026",
                        "title": "Title 1"
                      }
                    }
                  },
                  "newEntries": {
                    "local-id": {
                      "baseVersion": 1,
                      "entryType": "book",
                      "fields": {
                        "title": "New"
                      }
                    }
                  },
                  "removedEntries": {
                    "2": 5
                  },
                  "metaData": {
                    "databaseType": "bibtex"
                  }
                }""";
        assertEquals(expected, Files.readString(directory.resolve(OfflineChanges.fileName(properties))));
    }

    @Test
    void recordedChangesSurviveReload() {
        BibEntry changed = sharedEntry(1, 3);
        BibEntry removed = sharedEntry(2, 1);
        BibEntry added = new BibEntry(StandardEntryType.Book).withField(StandardField.AUTHOR, "Ada");

        OfflineChanges changes = OfflineChanges.load(directory, properties);
        changes.recordChange(changed);
        changes.recordRemoval(List.of(removed));
        changes.recordInsert(List.of(added));
        changes.recordMetaData(Map.of("databaseType", "bibtex;"));

        OfflineChanges.Recorded recorded = OfflineChanges.load(directory, properties).peek();

        assertEquals(Map.of(1, new OfflineChanges.EntryState(3, "article", Map.of("title", "Title 1"))), recorded.changedEntries());
        assertEquals(Map.of(added.getId(), new OfflineChanges.EntryState(1, "book", Map.of("author", "Ada"))), recorded.newEntries());
        assertEquals(Map.of(2, 1), recorded.removedEntries());
        assertEquals(Map.of("databaseType", "bibtex;"), recorded.metaData());
    }

    @Test
    void peekKeepsEverythingUntilItIsForgotten() throws Exception {
        BibEntry changed = sharedEntry(1, 1);
        OfflineChanges changes = OfflineChanges.load(directory, properties);
        changes.recordChange(changed);
        changes.recordRemoval(List.of(sharedEntry(2, 1)));
        changes.recordMetaData(Map.of("databaseType", "bibtex;"));

        changes.peek();

        assertFalse(changes.isEmpty());
        assertFalse(OfflineChanges.load(directory, properties).isEmpty());

        changes.forget(changed);
        changes.forgetRemovals(Set.of(2));
        changes.forgetMetaData();

        assertTrue(changes.isEmpty());
        assertTrue(OfflineChanges.load(directory, properties).isEmpty());
        assertFalse(Files.exists(directory.resolve(OfflineChanges.fileName(properties))));
    }

    @Test
    void laterChangesKeepTheBaseVersionOfTheFirst() {
        BibEntry entry = sharedEntry(1, 4);
        OfflineChanges changes = OfflineChanges.load(directory, properties);
        changes.recordChange(entry);
        entry.setField(StandardField.TITLE, "Edited twice");

        changes.recordChange(entry);

        OfflineChanges.EntryState state = changes.peek().changedEntries().get(1);
        assertEquals(4, state.baseVersion());
        assertEquals("Edited twice", state.fields().get("title"));
    }

    @Test
    void removalOfNeverSharedEntryLeavesNoTrace() {
        BibEntry added = new BibEntry(StandardEntryType.Book);
        OfflineChanges changes = OfflineChanges.load(directory, properties);
        changes.recordInsert(List.of(added));

        changes.recordRemoval(List.of(added));

        assertTrue(changes.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"changedEntries\": null}", "not json at all"})
    void unusableFileLoadsAsEmpty(String content) throws Exception {
        Files.writeString(directory.resolve(OfflineChanges.fileName(properties)), content);

        assertTrue(OfflineChanges.load(directory, properties).isEmpty());
    }

    @Test
    void secondSessionDoesNotOverwriteTheRecordsOfTheFirst() {
        OfflineChanges first = OfflineChanges.load(directory, properties);
        OfflineChanges second = OfflineChanges.load(directory, properties);

        first.recordChange(sharedEntry(1, 1));
        second.recordChange(sharedEntry(2, 1));

        assertEquals(Set.of(1, 2), OfflineChanges.load(directory, properties).peek().changedEntries().keySet());
    }

    @Test
    void differentDatabasesUseDifferentFiles() {
        DBMSConnectionProperties other = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setHost("localhost")
                .setPort(5432)
                .setDatabase("other")
                .setUser("alice")
                .setPassword("secret")
                .createDBMSConnectionProperties();
        OfflineChanges.load(directory, properties).recordChange(sharedEntry(1, 1));

        assertTrue(OfflineChanges.load(directory, other).isEmpty());
    }
}
