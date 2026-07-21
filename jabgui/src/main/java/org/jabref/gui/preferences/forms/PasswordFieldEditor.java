package org.jabref.gui.preferences.forms;

import java.util.function.Consumer;

import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.util.ControlHelper;

import com.dlsc.gemsfx.EnhancedPasswordField;

/// Custom editor: a bound {@link EnhancedPasswordField} with an optional row of icon buttons in its
/// right slot. {@link EnhancedPasswordField} is third-party, so the fluent subject is this wrapper
/// and {@link #build()} hands back the field:
///
/// ```java
/// PasswordFieldEditor.create(viewModel.apiKeyProperty())
///                    .withRevealButton()
///                    .withClearButton()
///                    .build();
/// ```
///
/// Every button follows the field's own disabled state, so disabling the field disables the row.
public final class PasswordFieldEditor {

    private final EnhancedPasswordField field = new EnhancedPasswordField();
    private final HBox buttons = new HBox();

    private PasswordFieldEditor(StringProperty value) {
        field.textProperty().bindBidirectional(value);
        field.setRight(buttons);
    }

    public static PasswordFieldEditor create(StringProperty value) {
        return new PasswordFieldEditor(value);
    }

    /// Toggles between masked and plain text.
    public PasswordFieldEditor withRevealButton() {
        return withIconButton(IconTheme.JabRefIcons.PASSWORD_REVEALED,
                passwordField -> passwordField.setShowPassword(!passwordField.isShowPassword()));
    }

    /// Empties the field and returns focus to it.
    public PasswordFieldEditor withClearButton() {
        return withIconButton(IconTheme.JabRefIcons.DELETE_ENTRY, passwordField -> {
            passwordField.clear();
            passwordField.requestFocus();
        });
    }

    public PasswordFieldEditor withIconButton(JabRefIcon icon, Consumer<EnhancedPasswordField> action) {
        Button button = ControlHelper.iconButton(icon);
        button.disableProperty().bind(field.disableProperty());
        button.setOnAction(_ -> action.accept(field));
        buttons.getChildren().add(button);
        return this;
    }

    /// Reveals on a click anywhere in the field, with a non-interactive icon as the only affordance.
    /// Kept for the fields that shipped with this behaviour; prefer {@link #withRevealButton()}.
    public PasswordFieldEditor withRevealOnClick() {
        buttons.getChildren().add(IconTheme.JabRefIcons.PASSWORD_REVEALED.getGraphicNode());
        field.setOnMouseClicked(_ -> field.setShowPassword(!field.isShowPassword()));
        return this;
    }

    public EnhancedPasswordField build() {
        return field;
    }
}
