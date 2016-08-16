package net.sf.jabref.specialfields;

import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class SpecialFieldsUtilsTest {

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testSyncKeywordsFromSpecialFieldsBibEntry() {
        BibEntry entry = new BibEntry();
        entry.setField("ranking", "rank2");
        SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry);
        assertEquals(Optional.of("rank2"), entry.getFieldOptional("keywords"));
    }

    @Test
    public void testSyncKeywordsFromSpecialFieldsBibEntryNamedCompoundHasEdits() {
        BibEntry entry = new BibEntry();
        NamedCompound nc = new NamedCompound("Test");
        entry.setField("ranking", "rank2");
        SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, nc);
        assertTrue(nc.hasEdits());
    }

    @Test
    public void testSyncKeywordsFromSpecialFieldsBibEntryExisitingKeyword() {
        BibEntry entry = new BibEntry();
        entry.setField("ranking", "rank2");
        entry.setField("keywords", "rank3");
        SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry);
        assertEquals(Optional.of("rank2"), entry.getFieldOptional("keywords"));
    }

    @Test
    public void testSyncKeywordsFromSpecialFieldsBibEntryNamedCompoundCorrectContent() {
        BibEntry entry = new BibEntry();
        NamedCompound nc = new NamedCompound("Test");
        entry.setField("ranking", "rank2");
        SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, nc);
        assertEquals(Optional.of("rank2"), entry.getFieldOptional("keywords"));
    }

    @Test
    public void testSyncKeywordsFromSpecialFieldsBibEntryNamedCompoundNoEdits() {
        BibEntry entry = new BibEntry();
        NamedCompound nc = new NamedCompound("Test");
        SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, nc);
        assertFalse(nc.hasEdits());
    }

    @Test
    public void testSyncSpecialFieldsFromKeywordsBibEntry() {
        BibEntry entry = new BibEntry();
        entry.setField("keywords", "rank2");
        SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry);
        assertEquals(Optional.of("rank2"), entry.getFieldOptional("ranking"));
    }

    @Test
    public void testSyncSpecialFieldsFromKeywordsBibEntryNamedCompoundHasEdits() {
        BibEntry entry = new BibEntry();
        NamedCompound nc = new NamedCompound("Test");
        entry.setField("keywords", "rank2");
        SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, nc);
        assertTrue(nc.hasEdits());
    }

    @Test
    public void testSyncSpecialFieldsFromKeywordsBibEntryNamedCompoundCorrectContent() {
        BibEntry entry = new BibEntry();
        NamedCompound nc = new NamedCompound("Test");
        entry.setField("keywords", "rank2");
        SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, nc);
        assertEquals(Optional.of("rank2"), entry.getFieldOptional("ranking"));
    }

    @Test
    public void testSyncSpecialFieldsFromKeywordsBibEntryNamedCompoundNoEdit() {
        BibEntry entry = new BibEntry();
        NamedCompound nc = new NamedCompound("Test");
        SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, nc);
        assertFalse(nc.hasEdits());
    }

    @Test
    public void testGetSpecialFieldInstanceFromFieldNameValid() {
        assertEquals(Optional.of(Rank.getInstance()),
                SpecialFieldsUtils.getSpecialFieldInstanceFromFieldName("ranking"));
    }

    @Test
    public void testGetSpecialFieldInstanceFromFieldNameInvalid() {
        assertEquals(Optional.empty(), SpecialFieldsUtils.getSpecialFieldInstanceFromFieldName("title"));
    }

    @Test
    public void testIsSpecialFieldTrue() {
        assertTrue(SpecialFieldsUtils.isSpecialField("ranking"));
    }

    @Test
    public void testIsSpecialFieldFalse() {
        assertFalse(SpecialFieldsUtils.isSpecialField("title"));
    }
}
