package org.jabref.gui.search;

import javafx.scene.input.KeyEvent;

import org.jabref.gui.Globals;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;

import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.LoggerFactory;

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

            LoggerFactory.getLogger(SearchTextField.class).warn("event: {}", event);
            LoggerFactory.getLogger(SearchTextField.class).warn("bindings: {}", Globals.getKeyPrefs().mapToKeyBindings(event));

            // Per default ESC is bound to both CLOSE_DIALGUE and CLEAR_SEARcH
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
