package org.jabref.gui.mergeentries;

import org.jabref.logic.l10n.Localization;

public enum PlainTextOrDiff {
    PLAIN_TEXT(Localization.lang("Plain Text")), Diff(Localization.lang("Show Diff"));

    private final String value;

    PlainTextOrDiff(String value) {
        this.value = value;
    }

    public static PlainTextOrDiff parse(String name) {
        try {
            return PlainTextOrDiff.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Diff; // default
        }
    }

    public String getValue() {
        return value;
    }

    public static PlainTextOrDiff fromString(String str) {
        return Enum.valueOf(PlainTextOrDiff.class, str);
    }
}
