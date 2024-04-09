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
import org.jabref.model.metadata.MetaData;
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
    private String txtFileDirPath;
    private Collection<BibEntry> entries;

    private BibEntry entry1;
    private BibEntry entry2;
    private Path txtFileDir;
    private MetaData metaData;
    private BibDatabaseContext databaseContext;
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
        txtFileDirPath = txtFileDir.toAbsolutePath().toString();
        Files.write(txtFileDir, Collections.singleton("x.txt file contents for test"));

        entry1 = database.getEntryByCitationKey("entry1")
                .orElseThrow(() -> new Exception("Entry with citation key 'entry1' not found"));

        entry2 = database.getEntryByCitationKey("entry2")
                .orElseThrow(() -> new Exception("Entry with citation key 'entry2' not found"));

        BibEntry entry3 = new BibEntry().withField(StandardField.FILE, txtFileDirPath);
        BibEntry entry4 = new BibEntry().withField(StandardField.FILE, "");

        List<BibEntry> entries = List.of(entry1, entry2, entry3, entry4);
        database = new BibDatabase(entries);

        filePreferences = mock(FilePreferences.class);
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(txtFileDir.toAbsolutePath()));
        when(filePreferences.getUserAndHost()).thenReturn("User124");

        metaData = new MetaData();
        metaData.setDefaultFileDirectory(txtFileDirPath);
        metaData.setUserFileDirectory("User124", txtFileDirPath);

        databaseContext = new BibDatabaseContext(database, metaData, txtFileDir.toAbsolutePath());
        databaseContext.setDatabasePath(txtFileDir);

        fileLookup = new DatabaseFileLookup(databaseContext, filePreferences);
    }

    /**
     * Tests the prerequisites of this test-class itself.
     */
    @Test
    void prerequisitesFulfilled() {
        assertEquals(4, database.getEntryCount());
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
        assertNotNull(fileLookup.getPathOfDatabase());
    }

    /**
     * y.txt should not be found in any directory.
     */
    @Test
    void fileShouldNotBeFound() {
        assertFalse(fileLookup.lookupDatabase(tempDir.resolve("y.txt")));
    }

    /**
     * x.txt should be found in the user-specific directory
     */
    @Test
    void userSpecificDirectory() {
        assertEquals(metaData.getUserFileDirectory(filePreferences.getUserAndHost()), Optional.of(txtFileDirPath));
    }

    /**
     * x.txt should be found in the general directory
     */
    @Test
    void defaultFileDirectory() {
        assertEquals(metaData.getDefaultFileDirectory(), Optional.of(txtFileDirPath));
    }

    /**
     * x.txt should be found in the main file directory
     */
    @Test
    void mainFileDirectory() {
        assertEquals(filePreferences.getMainFileDirectory(), Optional.of(txtFileDir.toAbsolutePath()));
    }

    /**
     * x.txt should be found in the database file directory
     */
    @Test
    void bibFileDirectory() {
        assertEquals(databaseContext.getDatabasePath(), Optional.of(txtFileDir.toAbsolutePath()));
    }
}
