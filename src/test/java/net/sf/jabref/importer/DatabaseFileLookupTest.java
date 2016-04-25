package net.sf.jabref.importer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
    public void setUp() throws FileNotFoundException, IOException {
        Globals.prefs = JabRefPreferences.getInstance();

        try (FileInputStream stream = new FileInputStream(ImportDataTest.UNLINKED_FILES_TEST_BIB);
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            ParserResult result = BibtexParser.parse(reader);
            database = result.getDatabase();
            entries = database.getEntries();

            entry1 = database.getEntryByKey("entry1");
            entry2 = database.getEntryByKey("entry2");
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
