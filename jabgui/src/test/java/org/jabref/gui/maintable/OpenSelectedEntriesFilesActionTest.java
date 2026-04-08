package org.jabref.gui.maintable;

import java.nio.file.Path;
import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenSelectedEntriesFilesActionTest {

    @Test
    void getLinkedFilesToOpenReturnsEntryFilesWhenPresent() {
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase());
        LinkedFile childFile = new LinkedFile("", Path.of("child.pdf"), "pdf");
        BibEntry childEntry = new BibEntry()
                .withField(StandardField.CROSSREF, "parent")
                .withFiles(List.of(childFile));

        List<LinkedFile> files = OpenSelectedEntriesFilesAction.getLinkedFilesToOpen(childEntry, databaseContext);

        assertEquals(1, files.size());
        assertEquals("child.pdf", files.getFirst().getLink());
    }

    @Test
    void getLinkedFilesToOpenReturnsParentFilesViaCrossref() {
        BibDatabase database = new BibDatabase();
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);

        LinkedFile parentFile = new LinkedFile("", Path.of("proceedings.pdf"), "pdf");
        BibEntry parentEntry = new BibEntry()
                .withCitationKey("parent")
                .withFiles(List.of(parentFile));
        BibEntry childEntry = new BibEntry().withField(StandardField.CROSSREF, "parent");

        database.insertEntries(List.of(parentEntry, childEntry));

        List<LinkedFile> files = OpenSelectedEntriesFilesAction.getLinkedFilesToOpen(childEntry, databaseContext);

        assertEquals(1, files.size());
        assertEquals("proceedings.pdf", files.getFirst().getLink());
    }

    @Test
    void getLinkedFilesToOpenReturnsEmptyWhenCrossrefCannotBeResolved() {
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase());
        BibEntry childEntry = new BibEntry().withField(StandardField.CROSSREF, "missing-parent");

        List<LinkedFile> files = OpenSelectedEntriesFilesAction.getLinkedFilesToOpen(childEntry, databaseContext);

        assertTrue(files.isEmpty());
    }

    @Test
    void getLinkedFilesToOpenReturnsEmptyWhenResolvedParentHasNoFiles() {
        BibDatabase database = new BibDatabase();
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);

        BibEntry parentEntry = new BibEntry().withCitationKey("parent");
        BibEntry childEntry = new BibEntry().withField(StandardField.CROSSREF, "parent");
        database.insertEntries(List.of(parentEntry, childEntry));

        List<LinkedFile> files = OpenSelectedEntriesFilesAction.getLinkedFilesToOpen(childEntry, databaseContext);

        assertTrue(files.isEmpty());
    }
}
