package net.sf.jabref.logic.specialfields;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.specialfields.Rank;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class SpecialFieldsUtilsTest {

    @Test
    public void testSyncKeywordsFromSpecialFieldsBibEntry() {
        BibEntry entry = new BibEntry();
        entry.setField("ranking", "rank2");
        SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, ", ");
        assertEquals(Optional.of("rank2"), entry.getField("keywords"));
    }

    @Test
    public void testSyncKeywordsFromSpecialFieldsBibEntryChangeListHasEdits() {
        BibEntry entry = new BibEntry();
        List<FieldChange> changeList = new ArrayList<>();
        entry.setField("ranking", "rank2");
        SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, changeList, ", ");
        assertFalse(changeList.isEmpty());
    }

    @Test
    public void testSyncKeywordsFromSpecialFieldsBibEntryExisitingKeyword() {
        BibEntry entry = new BibEntry();
        entry.setField("ranking", "rank2");
        entry.setField("keywords", "rank3");
        SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, ", ");
        assertEquals(Optional.of("rank2"), entry.getField("keywords"));
    }

    @Test
    public void testSyncKeywordsFromSpecialFieldsBibEntryChangeListCorrectContent() {
        BibEntry entry = new BibEntry();
        List<FieldChange> changeList = new ArrayList<>();
        entry.setField("ranking", "rank2");
        SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, changeList, ", ");
        assertEquals(Optional.of("rank2"), entry.getField("keywords"));
    }

    @Test
    public void testSyncKeywordsFromSpecialFieldsBibEntryChangeListNoEdits() {
        BibEntry entry = new BibEntry();
        List<FieldChange> changeList = new ArrayList<>();
        SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, changeList, ", ");
        assertTrue(changeList.isEmpty());
    }

    @Test
    public void testSyncSpecialFieldsFromKeywordsBibEntry() {
        BibEntry entry = new BibEntry();
        entry.setField("keywords", "rank2");
        SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry);
        assertEquals(Optional.of("rank2"), entry.getField("ranking"));
    }

    @Test
    public void testSyncSpecialFieldsFromKeywordsBibEntryChangeListHasEdits() {
        BibEntry entry = new BibEntry();
        List<FieldChange> changeList = new ArrayList<>();
        entry.setField("keywords", "rank2");
        SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, changeList);
        assertFalse(changeList.isEmpty());
    }

    @Test
    public void testSyncSpecialFieldsFromKeywordsBibEntryChangeListCorrectContent() {
        BibEntry entry = new BibEntry();
        List<FieldChange> changeList = new ArrayList<>();
        entry.setField("keywords", "rank2");
        SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, changeList);
        assertEquals(Optional.of("rank2"), entry.getField("ranking"));
    }

    @Test
    public void testSyncSpecialFieldsFromKeywordsBibEntryChangeListNoEdit() {
        BibEntry entry = new BibEntry();
        List<FieldChange> changeList = new ArrayList<>();
        SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, changeList);
        assertTrue(changeList.isEmpty());
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
