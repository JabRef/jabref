package org.jabref.logic.importer;

import java.util.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.nio.file.Files;
import java.nio.file.Path;

class DatabaseFileLookupTest {

    private BibDatabase database;
    private Collection<BibEntry> entries;

    private BibEntry entry1;
    private BibEntry entry2;
    private Path tempDir;
    private Path txtFileDir;
    private FilePreferences filePreferences;
    private static DatabaseFileLookup fileLookup;

    /**
     * Sets up the test environment before each test case.
     *
     * @throws Exception if an error occurs during setup
     */
    @BeforeEach
    void setUp() throws Exception {
        ParserResult result = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS),
                new DummyFileUpdateMonitor())
                .importDatabase(ImportDataTest.UNLINKED_FILES_TEST_BIB);
        database = result.getDatabase();
        entries = database.getEntries();

        tempDir = Files.createTempDirectory("testDir");
        txtFileDir = tempDir.resolve("x.txt");

        entry1 = database.getEntryByCitationKey("entry1").get();
        entry2 = database.getEntryByCitationKey("entry2").get();

        BibEntry entry3 = new BibEntry().withField(StandardField.FILE, txtFileDir.toAbsolutePath().toString());
        BibEntry entry4 = new BibEntry().withField(StandardField.FILE, "");

        List<BibEntry> entries = new ArrayList<>(Arrays.asList(entry1, entry2, entry3, entry4));
        ObservableList<BibEntry> observableEntryList = FXCollections
                .synchronizedObservableList(FXCollections.observableArrayList(entries));
        BibDatabase databaseMock = mock(BibDatabase.class);
        when(databaseMock.getEntries()).thenReturn(observableEntryList);

        Files.write(txtFileDir, Collections.singleton("x.txt file contents for test"));

        filePreferences = mock(FilePreferences.class);
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(txtFileDir.toAbsolutePath()));

        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getFileDirectories(filePreferences))
                .thenReturn(Collections.singletonList(txtFileDir));
        when(databaseContext.getDatabase()).thenReturn(databaseMock);
        when(databaseContext.getDatabase().getEntries()).thenReturn(observableEntryList);
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
        assertEquals(filePreferences.getMainFileDirectory().orElse(Path.of("")).toString(),
                txtFileDir.toAbsolutePath().toString());
        assertNotNull(fileLookup.getPathOfDatabase());
    }

    /**
     *
     * y.txt should not be found in the any directory.
     */
    @Test
    void fileShouldNotBeFound() {
        assertFalse(fileLookup.lookupDatabase(tempDir.resolve("y.txt")));
    }
}
