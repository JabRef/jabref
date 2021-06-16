package org.jabref.gui.keyboard;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;

public enum KeyBindingCategory {

    FILE(Localization.lang("File")),
    EDIT(Localization.lang("Edit")),
    SEARCH(Localization.lang("Search")),
    VIEW(Localization.lang("View")),
    BIBTEX(BibDatabaseMode.BIBTEX.getFormattedName()),
    QUALITY(Localization.lang("Quality")),
    TOOLS(Localization.lang("Tools")),
    EDITOR(Localization.lang("Text editor"));

    private final String name;

    KeyBindingCategory(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
