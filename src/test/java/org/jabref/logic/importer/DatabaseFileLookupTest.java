package org.jabref.logic.importer;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class DatabaseFileLookupTest {

    private BibDatabase database;
    private Collection<BibEntry> entries;

    private BibEntry entry1;
    private BibEntry entry2;

    @BeforeEach
    void setUp() throws Exception {
        ParserResult result = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor())
                .importDatabase(ImportDataTest.UNLINKED_FILES_TEST_BIB, StandardCharsets.UTF_8);
        database = result.getDatabase();
        entries = database.getEntries();

        entry1 = database.getEntryByCitationKey("entry1").get();
        entry2 = database.getEntryByCitationKey("entry2").get();
    }

    /**
     * Tests the prerequisites of this test-class itself.
     */
    @Test
    void testTestDatabase() {
        assertEquals(2, database.getEntryCount());
        assertEquals(2, entries.size());
        assertNotNull(entry1);
        assertNotNull(entry2);
    }
}
