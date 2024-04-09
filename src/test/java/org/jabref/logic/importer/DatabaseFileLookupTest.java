package org.jabref.logic.importer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.util.io.DatabaseFileLookup;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseFileLookupTest {

    @TempDir
    Path tempDir;
    private BibDatabase database;
    private Collection<BibEntry> entries;

    private BibEntry entry1;
    private BibEntry entry2;
    private Path txtFileDir;
    private FilePreferences filePreferences;
    private DatabaseFileLookup fileLookup;

    @BeforeEach
    void setUp() throws Exception {
        ParserResult result = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS),
                new DummyFileUpdateMonitor())
                .importDatabase(ImportDataTest.UNLINKED_FILES_TEST_BIB);
        database = result.getDatabase();
        entries = database.getEntries();

        tempDir = Files.createTempDirectory("testDir");
        txtFileDir = tempDir.resolve("x.txt");
        Files.write(txtFileDir, Collections.singleton("x.txt file contents for test"));

        entry1 = database.getEntryByCitationKey("entry1")
                .orElseThrow(() -> new Exception("Entry with citation key 'entry1' not found"));

        entry2 = database.getEntryByCitationKey("entry2")
                .orElseThrow(() -> new Exception("Entry with citation key 'entry2' not found"));

        BibEntry entry3 = new BibEntry().withField(StandardField.FILE, txtFileDir.toAbsolutePath().toString());
        BibEntry entry4 = new BibEntry().withField(StandardField.FILE, "");

        List<BibEntry> entries = List.of(entry1, entry2, entry3, entry4);
        BibDatabase database = new BibDatabase(entries);

        filePreferences = mock(FilePreferences.class);
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(txtFileDir.toAbsolutePath()));

        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getFileDirectories(filePreferences))
                .thenReturn(Collections.singletonList(txtFileDir));
        when(databaseContext.getDatabase()).thenReturn(database);
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(txtFileDir.toAbsolutePath()));

        fileLookup = new DatabaseFileLookup(databaseContext, filePreferences);
    }

    /**
     * Tests the prerequisites of this test-class itself.
     */
    @Test
    void prerequisitesFulfilled() {
        assertEquals(2, database.getEntryCount());
        assertEquals(2, entries.size());
        assertNotNull(entry1);
        assertNotNull(entry2);
    }

    /**
     * x.txt should be found in the given directory.
     */
    @Test
    void fileShouldBeFound() {
        assertTrue(fileLookup.lookupDatabase(txtFileDir));
        assertEquals(filePreferences.getMainFileDirectory(), Optional.of(txtFileDir.toAbsolutePath()));
        assertNotNull(fileLookup.getPathOfDatabase());
    }

    /**
     * y.txt should not be found in any directory.
     */
    @Test
    void fileShouldNotBeFound() {
        assertFalse(fileLookup.lookupDatabase(tempDir.resolve("y.txt")));
    }
}
