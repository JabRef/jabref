package org.jabref.logic.openoffice.style;

import org.jabref.logic.citationstyle.CitationStyle;

public sealed interface OOStyle permits CitationStyle, JStyle, BstStyle {
    String getName();

    boolean isInternalStyle();

    String getPath();
}
