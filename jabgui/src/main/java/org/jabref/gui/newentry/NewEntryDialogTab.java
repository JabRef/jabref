package org.jabref.gui.newentry;

public enum NewEntryDialogTab {
    CHOOSE_ENTRY_TYPE,
    ENTER_IDENTIFIER,
    INTERPRET_CITATIONS,
    SPECIFY_BIBTEX,
    // Appended after the pre-existing constants (rather than grouped next to ENTER_IDENTIFIER, which is where it
    // appears in NewEntry.fxml's tab order -- unrelated to this enum's declaration order) because
    // JabRefGuiPreferences persists the last-used tab as the ordinal index into values(). Inserting a constant in
    // the middle would silently shift every later constant's ordinal, remapping existing users' stored preference
    // to the wrong tab.
    ENTER_URL,
}
