package org.jabref.logic.importer;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;

/**
 * @author Nosh&Dan
 * @version 09.11.2008 | 21:06:17
 */
public class DatabaseFileLookupTest {

    private BibDatabase database;
    private Collection<BibEntry> entries;

    private BibEntry entry1;
    private BibEntry entry2;


    @Before
    public void setUp() throws Exception {
        try (FileInputStream stream = new FileInputStream(ImportDataTest.UNLINKED_FILES_TEST_BIB);
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS)).parse(reader);
            database = result.getDatabase();
            entries = database.getEntries();

            entry1 = database.getEntryByKey("entry1").get();
            entry2 = database.getEntryByKey("entry2").get();
        }
    }

    /**
     * Tests the prerequisites of this test-class itself.
     */
    @Test
    public void testTestDatabase() {
        Assert.assertEquals(2, database.getEntryCount());
        Assert.assertEquals(2, entries.size());
        Assert.assertNotNull(entry1);
        Assert.assertNotNull(entry2);
    }

}
