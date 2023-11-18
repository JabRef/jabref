package org.jabref.gui.search;

import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;

public class SearchTextField {

    public static CustomTextField create() {
        CustomTextField textField = (CustomTextField) TextFields.createClearableTextField();
        textField.setPromptText(Localization.lang("Search") + "...");
        textField.setLeft(IconTheme.JabRefIcons.SEARCH.getGraphicNode());
        textField.setId("searchField");
        return textField;
    }
}
