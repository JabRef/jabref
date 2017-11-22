package org.jabref.gui.search;

import java.util.function.Supplier;

import org.jabref.logic.l10n.Localization;

/**
 * Collects the possible search modes
 */
public enum SearchDisplayMode {

    FLOAT(() -> Localization.lang("Float"), () -> Localization.lang("Gray out non-hits")),
    FILTER(() -> Localization.lang("Filter"), () -> Localization.lang("Hide non-hits"));

    private final Supplier<String> displayName;
    private final Supplier<String> toolTipText;

    /**
     * We have to use supplier for the localized text so that language changes are correctly reflected.
     */
    SearchDisplayMode(Supplier<String> displayName, Supplier<String> toolTipText) {
        this.displayName = displayName;
        this.toolTipText = toolTipText;
    }

    public String getDisplayName() {
        return displayName.get();
    }

    public String getToolTipText() {
        return toolTipText.get();
    }

}
