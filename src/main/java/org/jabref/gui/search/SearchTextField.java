package org.jabref.gui.search;

import javafx.scene.Node;
import javafx.scene.control.TextField;

import org.jabref.gui.IconTheme;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.l10n.Localization;

import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

public class SearchTextField {

    public static TextField create() {
        CustomTextField textField = (CustomTextField) TextFields.createClearableTextField();
        textField.setPromptText(Localization.lang("Search") + "...");
        Node node = IconTheme.JabRefIcon.SEARCH.getGraphicNode();
        node.setStyle("-fx-text-fill: #00ffff");
        textField.setLeft(node);
        return textField;
    }

    public static void switchSearchColor(TextField textField, boolean grammarBasedSearch) {
        if (grammarBasedSearch) {
            DefaultTaskExecutor.runInJavaFXThread(() ->
                    ((CustomTextField) textField).setLeft(IconTheme.JabRefIcon.ADVANCED_SEARCH.getGraphicNode()));
        } else {
            DefaultTaskExecutor.runInJavaFXThread(() ->
                    ((CustomTextField) textField).setLeft(IconTheme.JabRefIcon.SEARCH.getGraphicNode()));
        }
    }


}
