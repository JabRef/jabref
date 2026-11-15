package org.jabref.gui.consistency;

import java.util.Arrays;
import java.util.Optional;

import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;

public enum ConsistencySymbol {
    REQUIRED_FIELD_AT_ENTRY_TYPE_CELL_ENTRY("x", IconTheme.JabRefIcons.CONSISTENCY_REQUIRED_FIELD, Localization.lang("Required field is present")),
    OPTIONAL_FIELD_AT_ENTRY_TYPE_CELL_ENTRY("o", IconTheme.JabRefIcons.CONSISTENCY_OPTIONAL_FIELD, Localization.lang("Optional field is present")),
    UNKNOWN_FIELD_AT_ENTRY_TYPE_CELL_ENTRY("?", IconTheme.JabRefIcons.CONSISTENCY_UNKNOWN_FIELD, Localization.lang("Unknown field is present")),
    UNSET_FIELD_AT_ENTRY_TYPE_CELL_ENTRY("-", IconTheme.JabRefIcons.CONSISTENCY_UNSET_FIELD, Localization.lang("Field is absent"));

    private final String text;
    private final IconTheme.JabRefIcons icon;
    private final String description;

    ConsistencySymbol(String text, IconTheme.JabRefIcons icon, String description) {
        this.text = text;
        this.icon = icon;
        this.description = description;
    }

    public String getText() {
        return text;
    }

    /// The explanation shown in the symbols legend and as cell tooltip
    public String getDescription() {
        return description;
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
