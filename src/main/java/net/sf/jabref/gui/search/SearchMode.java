package net.sf.jabref.gui.search;

import net.sf.jabref.logic.l10n.Localization;

/**
 * Collects the possible search modes
 */
public enum SearchMode {
    FLOAT(Localization.lang("Float"),
            Localization.lang("Gray out non-matching entries")),
    FILTER(Localization.lang("Filter"),
            Localization.lang("Hide non-matching entries")),
    GLOBAL(
            Localization.lang("Global search"),
            Localization.lang("Search in all open databases"));

    private String displayName;
    private String toolTipText;

    SearchMode(String displayName, String toolTipText) {
        this.displayName = displayName;
        this.toolTipText = toolTipText;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getToolTipText() {
        return toolTipText;
    }
}