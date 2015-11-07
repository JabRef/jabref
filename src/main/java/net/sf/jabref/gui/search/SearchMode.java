package net.sf.jabref.gui.search;

import net.sf.jabref.logic.l10n.Localization;

/**
 * Collects the possible search modes
 */
public enum SearchMode {
    Incremental(Localization.lang("Incremental"), Localization.lang("Incremental search")), Float(Localization.lang("Float"),
            Localization.lang("Gray out non-matching entries")), Filter(Localization.lang("Filter"),
                    Localization.lang("Hide non-matching entries")), LiveFilter(Localization.lang("Live filter"),
                            Localization.lang("Automatically hide non-matching entries")), ResultsInDialog(
                                    Localization.lang("Show results in dialog"),
                                    Localization.lang("Show search results in a window")), Global(
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