package org.jabref.logic.ocr;

import org.jabref.logic.l10n.Localization;

public enum PagesWithTextHandling {
    SKIP(Localization.lang("Skip pages with text")),
    FORCE(Localization.lang("Overwrite text in pages contain text")),
    REDO(Localization.lang("Redo text in pages contain OCRed text"));

    private final String displayName;

    PagesWithTextHandling(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PagesWithTextHandling safeValueOf(String name) {
        try {
            return PagesWithTextHandling.valueOf(name);
        } catch (IllegalArgumentException e) {
            return PagesWithTextHandling.SKIP;
        }
    }
}


