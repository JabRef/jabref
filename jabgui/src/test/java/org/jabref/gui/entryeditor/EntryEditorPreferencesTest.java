package org.jabref.gui.entryeditor;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EntryEditorPreferencesTest {

    @Test
    void defaultGeneralTabFollowsMainTab() {
        List<EntryEditorTabModel> defaults = EntryEditorPreferences.getDefault().getTabModels();

        int mainIndex = defaults.indexOf(new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.ALL_FIELDS, true));

        assertEquals(EntryEditorPreferences.getDefaultGeneralTab(), defaults.get(mainIndex + 1));
    }

    @Test
    void defaultGeneralTabResolvesFieldsForEmptyEntry() {
        assertFalse(EntryEditorPreferences.getDefaultGeneralTab().resolveFields(new BibEntry(StandardEntryType.Article)).isEmpty());
    }
}
