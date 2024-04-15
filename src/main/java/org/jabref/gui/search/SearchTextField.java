package org.jabref.gui.search;

import javafx.scene.Node;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.Globals;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;

import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

public class SearchTextField {

    public static CustomTextField create() {
        CustomTextField textField = (CustomTextField) TextFields.createClearableTextField();
        textField.setPromptText(Localization.lang("Search..."));
        textField.setId("searchField");
        textField.getStyleClass().add("search-field");

        Node graphicNode = IconTheme.JabRefIcons.SEARCH.getGraphicNode();
        graphicNode.getStyleClass().add("search-field-icon");
        textField.setLeft(graphicNode);

        textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Other key bindings are handled at org.jabref.gui.keyboard.TextInputKeyBindings
            // We need to handle clear search here to have the code "more clean"
            // Otherwise, we would have to add a new class for this and handle the case hitting that class in TextInputKeyBindings

            if (Globals.getKeyPrefs().matches(event, KeyBinding.CLEAR_SEARCH)) {
                       textField.clear();
                       event.consume();
            }
        });

        return textField;
    }
}
