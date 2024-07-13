package org.jabref.logic.openoffice.style;

import org.jabref.gui.openoffice.StyleSelectDialogViewModel.StyleType;
import org.jabref.logic.citationstyle.CitationStyle;

public class CSLStyle implements OOStyle {
    private final CitationStyle citationStyle;

    public CSLStyle(CitationStyle citationStyle) {
        this.citationStyle = citationStyle;
    }

    @Override
    public String getName() {
        return citationStyle.getTitle();
    }

    @Override
    public boolean isInternalStyle() {
        return false; // CSL styles are always external
    }

    @Override
    public StyleType getStyleType() {
        return StyleType.CSL;
    }

    public CitationStyle getCitationStyle() {
        return citationStyle;
    }
}
