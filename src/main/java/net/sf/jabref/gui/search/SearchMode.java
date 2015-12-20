package net.sf.jabref.gui.search;

import net.sf.jabref.logic.l10n.Localization;

/**
 * Collects the possible search modes
 */
public enum SearchMode {

    FLOAT(Localization.lang("Float"),
            Localization.lang("Gray out non-hits")),
    FILTER(Localization.lang("Filter"),
            Localization.lang("Hide non-hits"))
    ;

    private final String displayName;
    private final String toolTipText;

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