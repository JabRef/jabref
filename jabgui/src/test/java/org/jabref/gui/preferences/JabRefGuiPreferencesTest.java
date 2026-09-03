package org.jabref.gui.preferences;

import java.util.List;

import org.jabref.gui.entryeditor.EntryEditorTabModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JabRefGuiPreferencesTest {

    private static final EntryEditorTabModel PREVIEW =
            new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.PREVIEW, true);
    private static final EntryEditorTabModel MAIN =
            new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.ALL_FIELDS, true);
    private static final EntryEditorTabModel SOURCE =
            new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.SOURCE, true);

    @Test
    void storedTabOrderIsApplied() {
        List<EntryEditorTabModel> ordered = JabRefGuiPreferences.applyStoredTabOrder(
                List.of(PREVIEW, MAIN, SOURCE),
                List.of("SOURCE", "ALL_FIELDS"));

        assertEquals(List.of(PREVIEW, SOURCE, MAIN), ordered);
    }

    @Test
    void tabsUnknownToStoredOrderAreAppendedInDefaultOrder() {
        List<EntryEditorTabModel> ordered = JabRefGuiPreferences.applyStoredTabOrder(
                List.of(PREVIEW, MAIN, SOURCE),
                List.of("SOURCE"));

        assertEquals(List.of(PREVIEW, SOURCE, MAIN), ordered);
    }

    @Test
    void duplicateCustomTabNamesAreAllKept() {
        EntryEditorTabModel first = new EntryEditorTabModel.CustomizedFieldsTab("General", List.of("keywords"));
        EntryEditorTabModel second = new EntryEditorTabModel.CustomizedFieldsTab("General", List.of("doi"));

        List<EntryEditorTabModel> ordered = JabRefGuiPreferences.applyStoredTabOrder(
                List.of(PREVIEW, MAIN, first, second),
                List.of("custom:General", "ALL_FIELDS"));

        // The stored-order entry consumes the first duplicate; the second survives at the end.
        assertEquals(List.of(PREVIEW, first, MAIN, second), ordered);
    }
}
