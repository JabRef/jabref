package org.jabref.gui.consistency;

import java.util.Arrays;
import java.util.Optional;

import org.jabref.gui.icon.IconTheme;

public enum ConsistencySymbol {
    REQUIRED_FIELD_AT_ENTRY_TYPE_CELL_ENTRY("x", IconTheme.JabRefIcons.CONSISTENCY_REQUIRED_FIELD),
    OPTIONAL_FIELD_AT_ENTRY_TYPE_CELL_ENTRY("o", IconTheme.JabRefIcons.CONSISTENCY_OPTIONAL_FIELD),
    UNKNOWN_FIELD_AT_ENTRY_TYPE_CELL_ENTRY("?", IconTheme.JabRefIcons.CONSISTENCY_UNKNOWN_FIELD),
    UNSET_FIELD_AT_ENTRY_TYPE_CELL_ENTRY("-", IconTheme.JabRefIcons.CONSISTENCY_UNSET_FIELD);

    private final String text;
    private final IconTheme.JabRefIcons icon;

    ConsistencySymbol(String text, IconTheme.JabRefIcons icon) {
        this.text = text;
        this.icon = icon;
    }

    public String getText() {
        return text;
    }

    public IconTheme.JabRefIcons getIcon() {
        return icon;
    }

    public static Optional<ConsistencySymbol> fromText(String text) {
        return Arrays.stream(ConsistencySymbol.values())
                     .filter(symbol -> symbol.getText().equals(text))
                     .findFirst();
    }
}
