package net.sf.jabref;

/**
 * Collects the possible search modes
 */
public enum SearchMode {
    Incremental(Globals.lang("Incremental"), Globals.lang("Incremental search")), Float(Globals.lang("Float"),
            Globals.lang("Gray out non-matching entries")), Filter(Globals.lang("Filter"),
                    Globals.lang("Hide non-matching entries")), LiveFilter(Globals.lang("Live filter"),
                            Globals.lang("Automatically hide non-matching entries")), ResultsInDialog(
                                    Globals.lang("Show results in dialog"),
                                    Globals.lang("Show search results in a window")), Global(
                                            Globals.lang("Global search"),
                                            Globals.lang("Search in all open databases"));

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