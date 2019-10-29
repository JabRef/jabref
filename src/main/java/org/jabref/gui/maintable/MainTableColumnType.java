package org.jabref.gui.maintable;

import org.jabref.logic.l10n.Localization;

public enum MainTableColumnType {

    GROUPS("groups"),
    FILE("file"),
    EXTRAFILE("extrafile", Localization.lang("File type")),
    LINKED_IDENTIFIER("linked_id"),
    SPECIALFIELD("special", Localization.lang("Special")),
    NORMALFIELD("field");

    private String qualifier;
    private String displayName;

    MainTableColumnType(String qualifier) {
        this.qualifier = qualifier;
    }

    MainTableColumnType(String qualifier, String displayName) {
        this(qualifier);
        this.displayName = displayName;
    }

    public String getQualifier() { return qualifier; }

    public String getDisplayName() { return displayName; }

    public static MainTableColumnType parse(String text) {
        switch (text) {
            case "groups": return GROUPS;
            case "file": return FILE;
            case "extrafile": return EXTRAFILE;
            case "linked_id": return LINKED_IDENTIFIER;
            case "special": return SPECIALFIELD;

            case "field":
            default: return NORMALFIELD;
        }
    }
}
