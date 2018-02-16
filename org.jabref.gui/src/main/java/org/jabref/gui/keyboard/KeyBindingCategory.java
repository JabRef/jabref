package org.jabref.gui.keyboard;

import org.jabref.logic.l10n.Localization;

public enum KeyBindingCategory {

    FILE(Localization.lang("File")),
    EDIT(Localization.lang("Edit")),
    SEARCH(Localization.lang("Search")),
    VIEW(Localization.lang("View")),
    BIBTEX(Localization.BIBTEX),
    QUALITY(Localization.lang("Quality")),
    TOOLS(Localization.lang("Tools"));

    private final String name;

    private KeyBindingCategory(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
