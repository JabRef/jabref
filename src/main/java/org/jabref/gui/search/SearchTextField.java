package org.jabref.gui.search;

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
        textField.setLeft(IconTheme.JabRefIcons.SEARCH.getGraphicNode());
        textField.setId("searchField");

        textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Other key bindings are handled at org.jabref.gui.keyboard.TextInputKeyBindings
            // We need to handle clear search here to have the code "more clean"
            // Otherwise, we would have to add a new class for this and handle the case hitting that class in TextInputKeyBindings

            // As default, ESC is bound to both CLOSE_DIALOGUE and CLEAR_SEARCH
            Globals.getKeyPrefs().mapToKeyBindings(event)
                   .stream()
                   .filter(binding -> binding == KeyBinding.CLEAR_SEARCH)
                   .findFirst()
                   .ifPresent(binding -> {
                       textField.clear();
                       event.consume();
                   });
        });
        return textField;
    }
}
