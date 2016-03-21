package net.sf.jabref.model.database;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibtexEntryAssert;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.entry.BibEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.*;


public class BibDatabaseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance(); // set preferences for this test
    }

    @After
    public void tearDown() {
        Globals.prefs = null;
    }

    /**
     * Some basic test cases for resolving strings.
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    @Test
    public void resolveStrings() throws IOException {
        try (FileReader fr = new FileReader("src/test/resources/net/sf/jabref/util/twente.bib")) {
            ParserResult result = BibtexParser.parse(fr);

            BibDatabase db = result.getDatabase();

            assertEquals("Arvind", db.resolveForStrings("#Arvind#"));
            assertEquals("Patterson, David", db.resolveForStrings("#Patterson#"));
            assertEquals("Arvind and Patterson, David", db.resolveForStrings("#Arvind# and #Patterson#"));

            // Strings that are not found return just the given string.
            assertEquals("#unknown#", db.resolveForStrings("#unknown#"));
        }
    }

    @Test
    public void insertEntry() {
        BibDatabase database = new BibDatabase();
        assertEquals(Collections.emptyList(), database.getEntries());

        BibEntry entry = new BibEntry();
        database.insertEntry(entry);
        assertEquals(database.getEntries().size(), 1);
        BibtexEntryAssert.assertEquals(entry, database.getEntries().get(0));
    }

    @Test
    public void containsEntryId() {
        BibDatabase database = new BibDatabase();
        BibEntry entry = new BibEntry();
        assertFalse(database.containsEntryWithId(entry.getId()));
        database.insertEntry(entry);
        assertTrue(database.containsEntryWithId(entry.getId()));
    }

    @Test
    public void insertEntryWithSameIdThrowsException() {
        BibDatabase database = new BibDatabase();

        BibEntry entry0 = new BibEntry();
        database.insertEntry(entry0);

        BibEntry entry1 = new BibEntry(entry0.getId());
        thrown.expect(KeyCollisionException.class);
        database.insertEntry(entry1);
    }

    @Test
    public void removeEntry() {
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        database.insertEntry(entry);

        database.removeEntry(entry);
        assertEquals(Collections.emptyList(), database.getEntries());
        assertFalse(database.containsEntryWithId(entry.getId()));
    }

    @Test
    public void insertNullEntryThrowsException() {
        BibDatabase database = new BibDatabase();
        thrown.expect(NullPointerException.class);
        database.insertEntry(null);
    }

    @Test
    public void removeNullEntryThrowsException() {
        BibDatabase database = new BibDatabase();
        thrown.expect(NullPointerException.class);
        database.removeEntry(null);
    }

}
