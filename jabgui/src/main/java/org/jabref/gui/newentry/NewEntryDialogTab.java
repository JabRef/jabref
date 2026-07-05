package org.jabref.gui.newentry;

public enum NewEntryDialogTab {
    CHOOSE_ENTRY_TYPE,
    ENTER_IDENTIFIER,
    INTERPRET_CITATIONS,
    SPECIFY_BIBTEX,
    // New tabs must be appended at the end: the selected tab is persisted by its ordinal index
    // (see JabRefGuiPreferences#CREATE_ENTRY_APPROACH), so inserting in the middle would remap existing users' stored preference.
    ENTER_URL,
}
