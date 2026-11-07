package org.jabref.gui.preferences.forms;

import java.util.function.Consumer;

import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.util.ControlHelper;

import com.dlsc.gemsfx.EnhancedPasswordField;
import org.jspecify.annotations.NullMarked;

/// Custom editor: a bound [EnhancedPasswordField] with an optional row of icon buttons in its
/// right slot. [EnhancedPasswordField] is third-party, so the fluent subject is this wrapper;
/// [#field()] hands back the field once configuration is done — it does no work of its own,
/// it just stops `field` from being reachable before the withers have run:
///
/// ```java
/// PasswordFieldEditor.create(viewModel.apiKeyProperty())
///                    .withRevealButton()
///                    .withClearButton()
///                    .field();
/// ```
///
/// Every button follows the field's own disabled state, so disabling the field disables the row.
@NullMarked
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

    public EnhancedPasswordField field() {
        return field;
    }
}
