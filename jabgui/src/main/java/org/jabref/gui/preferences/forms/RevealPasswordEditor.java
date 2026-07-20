package org.jabref.gui.preferences.forms;

import javafx.beans.property.StringProperty;

import org.jabref.gui.icon.IconTheme;

import com.dlsc.gemsfx.EnhancedPasswordField;

/// Custom editor: a bound {@link EnhancedPasswordField} that toggles between masked and plain text
/// when its reveal icon is clicked. Used for proxy password and git PAT fields.
public final class RevealPasswordEditor {

    private RevealPasswordEditor() {
    }

    public static EnhancedPasswordField create(StringProperty value) {
        EnhancedPasswordField field = new EnhancedPasswordField();
        field.textProperty().bindBidirectional(value);
        field.setRight(IconTheme.JabRefIcons.PASSWORD_REVEALED.getGraphicNode());
        field.setOnMouseClicked(_ -> field.setShowPassword(!field.isShowPassword()));
        return field;
    }
}
