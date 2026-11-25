package org.jabref.logic.openoffice.style;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.bst.BstStyle;
import org.jabref.logic.openoffice.jstyle.JStyle;

public sealed interface OOStyle permits CitationStyle, JStyle, BstStyle {
    String getName();

    boolean isInternalStyle();

    String getPath();
}
