package org.jabref.gui.search;

import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;

import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

public class SearchTextField {

    public static CustomTextField create() {
        CustomTextField textField = (CustomTextField) TextFields.createClearableTextField();
        textField.setPromptText(Localization.lang("Search") + "...");
        textField.setLeft(IconTheme.JabRefIcons.SEARCH.getGraphicNode());
        return textField;
    }
}
