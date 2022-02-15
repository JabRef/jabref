package org.jabref.gui.mergeentries;

import org.jabref.logic.l10n.Localization;

public enum DiffMode {

    PLAIN(Localization.lang("None")),
    WORD(Localization.lang("Word by word")),
    CHARACTER(Localization.lang("Character by character")),
    WORD_SYMMETRIC(Localization.lang("Symmetric word by word")),
    CHARACTER_SYMMETRIC(Localization.lang("Symmetric character by character"));

    private final String text;

    DiffMode(String text) {
        this.text = text;
    }

    public static DiffMode parse(String name) {
        try {
            return DiffMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return WORD; // default
        }
    }

    public String getDisplayText() {
        return text;
    }
}
