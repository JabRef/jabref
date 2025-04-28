package org.jabref.logic.externalfiles;

import org.jabref.logic.l10n.Localization;

public enum ExternalFileSorter {
    DEFAULT(Localization.lang("Default")),
    DATE_ASCENDING(Localization.lang("Newest first")),
    DATE_DESCENDING(Localization.lang("Oldest first"));

    private final String sorter;

    ExternalFileSorter(String sorter) {
        this.sorter = sorter;
    }

    public static ExternalFileSorter parse(String name) {
        try {
            return ExternalFileSorter.valueOf(name);
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
    }

    public String getSorter() {
        return sorter;
    }
}
