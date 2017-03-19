package org.jabref.logic.specialfields;

import java.util.List;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class SpecialFieldsUtilsTest {

    @Test
    public void syncKeywordsFromSpecialFieldsWritesToKeywords() {
        BibEntry entry = new BibEntry();
        entry.setField("ranking", "rank2");
        SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, ',');
        assertEquals(Optional.of("rank2"), entry.getField("keywords"));
    }

    @Test
    public void syncKeywordsFromSpecialFieldsCausesChange() {
        BibEntry entry = new BibEntry();
        entry.setField("ranking", "rank2");
        List<FieldChange> changes = SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, ',');
        assertTrue(changes.size() > 0);
    }

    @Test
    public void syncKeywordsFromSpecialFieldsOverwritesKeywords() {
        BibEntry entry = new BibEntry();
        entry.setField("ranking", "rank2");
        entry.setField("keywords", "rank3");
        SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, ',');
        assertEquals(Optional.of("rank2"), entry.getField("keywords"));
    }

    @Test
    public void syncKeywordsFromSpecialFieldsForEmptyFieldCausesNoChange() {
        BibEntry entry = new BibEntry();
        List<FieldChange> changes = SpecialFieldsUtils.syncKeywordsFromSpecialFields(entry, ',');
        assertFalse(changes.size() > 0);
    }

    @Test
    public void syncSpecialFieldsFromKeywordWritesToSpecialField() {
        BibEntry entry = new BibEntry();
        entry.setField("keywords", "rank2");
        SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, ',');
        assertEquals(Optional.of("rank2"), entry.getField("ranking"));
    }

    @Test
    public void syncSpecialFieldsFromKeywordCausesChange() {
        BibEntry entry = new BibEntry();
        entry.setField("keywords", "rank2");
        List<FieldChange> changes = SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, ',');
        assertTrue(changes.size() > 0);
    }

    @Test
    public void syncSpecialFieldsFromKeywordCausesNoChangeWhenKeywordsAreEmpty() {
        BibEntry entry = new BibEntry();
        List<FieldChange> changes = SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, ',');
        assertFalse(changes.size() > 0);
    }
}
