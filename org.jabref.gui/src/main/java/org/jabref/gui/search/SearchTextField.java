package org.jabref.gui.search;

import javafx.scene.control.TextField;

import org.jabref.gui.IconTheme;
import org.jabref.logic.l10n.Localization;

import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

public class SearchTextField {

    public static TextField create() {
        CustomTextField textField = (CustomTextField) TextFields.createClearableTextField();
        textField.setPromptText(Localization.lang("Search") + "...");
        textField.setLeft(IconTheme.JabRefIcon.SEARCH.getGraphicNode());
        return textField;
    }
}
