package org.jabref.logic.specialfields;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.specialfields.SpecialField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    public void updateFieldRemovesSpecialFieldKeywordWhenKeywordSyncIsUsed() {
        BibEntry entry = new BibEntry();
        SpecialField specialField = SpecialField.PRINTED;
        Keyword specialFieldKeyword = specialField.getKeyWords().get(0);
        // Add the special field
        SpecialFieldsUtils.updateField(specialField, specialFieldKeyword.get(), entry, true, true, ',');
        // Remove it
        List<FieldChange> changes = SpecialFieldsUtils.updateField(specialField, specialFieldKeyword.get(), entry, true, true, ',');
        assertEquals(Arrays.asList(new FieldChange(entry, specialField.getFieldName(), specialFieldKeyword.get(), null),
                new FieldChange(entry, FieldName.KEYWORDS, specialFieldKeyword.get(), null)), changes);
        KeywordList remainingKeywords = entry.getKeywords(',');
        assertFalse(remainingKeywords.contains(specialFieldKeyword));
    }
}
