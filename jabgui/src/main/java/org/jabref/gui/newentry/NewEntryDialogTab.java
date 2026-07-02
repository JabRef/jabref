package org.jabref.gui.newentry;

public enum NewEntryDialogTab {
    // The order of these constants is persisted as an integer index (preference `CREATE_ENTRY_APPROACH`).
    // Append new tabs at the end so existing users' stored indices keep resolving to the same tab.
    CHOOSE_ENTRY_TYPE,
    ENTER_IDENTIFIER,
    INTERPRET_CITATIONS,
    SPECIFY_BIBTEX,
    ENTER_URL,
}
