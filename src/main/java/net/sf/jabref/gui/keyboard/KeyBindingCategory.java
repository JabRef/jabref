package net.sf.jabref.gui.keyboard;

import net.sf.jabref.logic.l10n.Localization;

public enum KeyBindingCategory {

    FILE(
            Localization.menuTitle("File")),
    EDIT(
            Localization.menuTitle("Edit")),
    SEARCH(
            Localization.menuTitle("Search")),
    VIEW(
            Localization.menuTitle("View")),
    BIBTEX(
            Localization.menuTitle("BibTeX")),
    QUALITY(
            Localization.menuTitle("Quality")),
    TOOLS(
            Localization.menuTitle("Tools"));

    private final String name;


    private KeyBindingCategory(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
