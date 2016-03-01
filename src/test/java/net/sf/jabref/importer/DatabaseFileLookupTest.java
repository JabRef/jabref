package net.sf.jabref.importer;

import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.io.FileReader;
import java.util.*;

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
        Globals.prefs = JabRefPreferences.getInstance();

        try (FileReader fr = new FileReader(ImportDataTest.UNLINKED_FILES_TEST_BIB)) {
            ParserResult result = BibtexParser.parse(fr);
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
    public void testTestDatabase() throws Exception {
        Assert.assertEquals(2, database.getEntryCount());
        Assert.assertEquals(2, entries.size());
        Assert.assertNotNull(entry1);
        Assert.assertNotNull(entry2);
    }

}
