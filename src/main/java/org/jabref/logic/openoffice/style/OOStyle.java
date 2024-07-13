package org.jabref.logic.openoffice.style;

import org.jabref.gui.openoffice.StyleSelectDialogViewModel.StyleType;

public interface OOStyle {
    String getName();

    boolean isInternalStyle();

    StyleType getStyleType();
}
