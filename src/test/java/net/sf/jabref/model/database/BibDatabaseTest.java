package net.sf.jabref.model.database;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibtexEntryAssert;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.IdGenerator;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
        try (FileInputStream stream = new FileInputStream("src/test/resources/net/sf/jabref/util/twente.bib");
                InputStreamReader fr = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
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
        assertEquals(database.getEntryCount(), 1);
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

    @Test(expected = KeyCollisionException.class)
    public void insertEntryWithSameIdThrowsException() {
        BibDatabase database = new BibDatabase();

        BibEntry entry0 = new BibEntry();
        database.insertEntry(entry0);

        BibEntry entry1 = new BibEntry(entry0.getId());
        database.insertEntry(entry1);
        fail();
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

    @Test(expected = NullPointerException.class)
    public void insertNullEntryThrowsException() {
        BibDatabase database = new BibDatabase();
        database.insertEntry(null);
        fail();
    }

    @Test(expected = NullPointerException.class)
    public void removeNullEntryThrowsException() {
        BibDatabase database = new BibDatabase();
        database.removeEntry(null);
        fail();
    }

    @Test
    public void emptyDatabaseHasNoStrings() {
        BibDatabase database = new BibDatabase();
        assertEquals(Collections.emptySet(), database.getStringKeySet());
        assertTrue(database.hasNoStrings());
    }

    @Test
    public void insertString() {
        BibDatabase database = new BibDatabase();
        BibtexString string = new BibtexString(IdGenerator.next(), "DSP", "Digital Signal Processing");
        database.addString(string);
        assertFalse(database.hasNoStrings());
        assertEquals(database.getStringKeySet().size(), 1);
        assertEquals(database.getStringCount(), 1);
        assertTrue(database.getStringValues().contains(string));
        assertTrue(database.getStringKeySet().contains(string.getId()));
        assertEquals(string, database.getString(string.getId()));
    }

    @Test
    public void insertAndRemoveString() {
        BibDatabase database = new BibDatabase();
        BibtexString string = new BibtexString(IdGenerator.next(), "DSP", "Digital Signal Processing");
        database.addString(string);
        database.removeString(string.getId());
        assertTrue(database.hasNoStrings());
        assertEquals(database.getStringKeySet().size(), 0);
        assertEquals(database.getStringCount(), 0);
        assertFalse(database.getStringValues().contains(string));
        assertFalse(database.getStringKeySet().contains(string.getId()));
        assertNull(database.getString(string.getId()));
    }

    @Test
    public void hasStringLabel() {
        BibDatabase database = new BibDatabase();
        BibtexString string = new BibtexString(IdGenerator.next(), "DSP", "Digital Signal Processing");
        database.addString(string);
        assertTrue(database.hasStringLabel("DSP"));
        assertFalse(database.hasStringLabel("VLSI"));
    }

    @Test(expected = KeyCollisionException.class)
    public void addSameStringLabelTwiceThrowsKeyCollisionException() {
        BibDatabase database = new BibDatabase();
        BibtexString string = new BibtexString(IdGenerator.next(), "DSP", "Digital Signal Processing");
        database.addString(string);
        string = new BibtexString(IdGenerator.next(), "DSP", "Digital Signal Processor");
        database.addString(string);
        fail();
    }

    @Test(expected = KeyCollisionException.class)
    public void addSameStringIdTwiceThrowsKeyCollisionException() {
        BibDatabase database = new BibDatabase();
        String id = IdGenerator.next();
        BibtexString string = new BibtexString(id, "DSP", "Digital Signal Processing");
        database.addString(string);
        string = new BibtexString(id, "VLSI", "Very Large Scale Integration");
        database.addString(string);
        fail();
    }
}
