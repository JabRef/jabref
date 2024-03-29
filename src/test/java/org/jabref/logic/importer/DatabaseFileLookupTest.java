package org.jabref.logic.importer;

import java.util.Collection;

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
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

class DatabaseFileLookupTest {

    private BibDatabase database;
    private Collection<BibEntry> entries;

    private BibEntry entry1;
    private BibEntry entry2;

    @BeforeEach
    void setUp() throws Exception {
        ParserResult result = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS),
                new DummyFileUpdateMonitor())
                .importDatabase(ImportDataTest.UNLINKED_FILES_TEST_BIB);
        database = result.getDatabase();
        entries = database.getEntries();

        entry1 = database.getEntryByCitationKey("entry1").get();
        entry2 = database.getEntryByCitationKey("entry2").get();
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
     * Tests the directory path functionality by creating a temporary file
     * directory,
     * creating a BibDatabaseContext with a BibDatabase containing two entries,
     * setting the temporary directory as the default file directory in the
     * preferences,
     * and creating a DatabaseFileLookup instance.
     *
     * @param tempDir the temporary directory path
     * @throws IOException if there is an error creating the temporary file
     *                     directory
     */
    @Test
    void directoryPathTests(@TempDir Path tempDir) throws IOException {
        Path txtFileDir = tempDir.resolve("x.txt"); // Create a temporary directory for testing

        try {
            Files.write(txtFileDir, Collections.singleton("x.txt file contents for test"));
        } catch (IOException e) {
            fail("Failed to create temporary file directory: " + e.getMessage());
        }

        // Create a BibDatabaseContext with a BibDatabase containing two entries
        BibDatabase bibDatabase = new BibDatabase();
        BibEntry entry1 = new BibEntry();
        entry1.setField(StandardField.FILE, txtFileDir.toAbsolutePath().toString());
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.FILE, "");
        bibDatabase.insertEntry(entry1);
        bibDatabase.insertEntry(entry2);

        BibDatabaseContext databaseContext = new BibDatabaseContext(bibDatabase);

        // Set the temporary directory as the default file directory
        // in the preferences and creating DatabaseFileLookup instance
        FilePreferences filePreferences = new FilePreferences("", txtFileDir.toAbsolutePath().toString(), false, "", "",
                false, false, null, Collections.emptySet(), false, null);
        DatabaseFileLookup fileLookup = new DatabaseFileLookup(databaseContext, filePreferences);

        // Tests
        assertTrue(fileLookup.lookupDatabase(txtFileDir)); // x.txt should be found
        assertFalse(fileLookup.lookupDatabase(tempDir.resolve("y.txt"))); // y.txt should not be found
        assertEquals(filePreferences.getMainFileDirectory().orElse(Path.of("")).toString(),
                txtFileDir.toAbsolutePath().toString());
        assertNotNull(fileLookup.getPathOfDatabase());
        assertEquals("", fileLookup.getPathOfDatabase().toString());
    }
}
