package net.sf.jabref.logic.specialfields;

import java.util.List;
import java.util.Optional;

import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.specialfields.SpecialField;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class SpecialFieldsUtilsTest {

    @Test
    public void testSyncKeywordsFromSpecialFieldsBibEntry() {
        BibEntry entry = new BibEntry();
        entry.setField("ranking", "rank2");
        SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, true, ',');
        assertEquals(Optional.of("rank2"), entry.getField("keywords"));
    }

    @Test
    public void testSyncKeywordsFromSpecialFieldsBibEntryNamedCompoundHasEdits() {
        BibEntry entry = new BibEntry();
        entry.setField("ranking", "rank2");
        List<FieldChange> changes = SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, true, ',');
        assertTrue(changes.size() > 0);
    }

    @Test
    public void testSyncKeywordsFromSpecialFieldsBibEntryExisitingKeyword() {
        BibEntry entry = new BibEntry();
        entry.setField("ranking", "rank2");
        entry.setField("keywords", "rank3");
        SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, true, ',');
        assertEquals(Optional.of("rank2"), entry.getField("keywords"));
    }

    @Test
    public void testSyncKeywordsFromSpecialFieldsBibEntryNamedCompoundCorrectContent() {
        BibEntry entry = new BibEntry();
        entry.setField("ranking", "rank2");
        SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, true, ',');
        assertEquals(Optional.of("rank2"), entry.getField("keywords"));
    }

    @Test
    public void testSyncKeywordsFromSpecialFieldsBibEntryNamedCompoundNoEdits() {
        BibEntry entry = new BibEntry();
        List<FieldChange> changes = SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, true, ',');
        assertFalse(changes.size() > 0);
    }

    @Test
    public void testSyncSpecialFieldsFromKeywordsBibEntry() {
        BibEntry entry = new BibEntry();
        entry.setField("keywords", "rank2");
        SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, ',');
        assertEquals(Optional.of("rank2"), entry.getField("ranking"));
    }

    @Test
    public void testSyncSpecialFieldsFromKeywordsBibEntryNamedCompoundHasEdits() {
        BibEntry entry = new BibEntry();
        entry.setField("keywords", "rank2");
        List<FieldChange> changes = SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, ',');
        assertTrue(changes.size() > 0);
    }

    @Test
    public void testSyncSpecialFieldsFromKeywordsBibEntryNamedCompoundCorrectContent() {
        BibEntry entry = new BibEntry();
        entry.setField("keywords", "rank2");
        SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, ',');
        assertEquals(Optional.of("rank2"), entry.getField("ranking"));
    }

    @Test
    public void testSyncSpecialFieldsFromKeywordsBibEntryNamedCompoundNoEdit() {
        BibEntry entry = new BibEntry();
        List<FieldChange> changes = SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, ',');
        assertFalse(changes.size() > 0);
    }

    @Test
    public void testGetSpecialFieldInstanceFromFieldNameValid() {
        assertEquals(Optional.of(SpecialField.RANK),
                SpecialField.getSpecialFieldInstanceFromFieldName("ranking"));
    }

    @Test
    public void testGetSpecialFieldInstanceFromFieldNameInvalid() {
        assertEquals(Optional.empty(), SpecialField.getSpecialFieldInstanceFromFieldName("title"));
    }

    @Test
    public void testIsSpecialFieldTrue() {
        assertTrue(SpecialField.isSpecialField("ranking"));
    }

    @Test
    public void testIsSpecialFieldFalse() {
        assertFalse(SpecialField.isSpecialField("title"));
    }
}
