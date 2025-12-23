package org.jabref.gui.util;

import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;

import com.airhacks.afterburner.injection.Injector;

public class BaseDialog<T> extends Dialog<T> {

    protected BaseDialog() {

        getDialogPane().getScene().setOnKeyPressed(event -> {
            KeyBindingRepository keyBindingRepository = Injector.instantiateModelOrService(KeyBindingRepository.class);
            if (keyBindingRepository.checkKeyCombinationEquality(KeyBinding.CLOSE, event)) {
                close();
            } else if (keyBindingRepository.checkKeyCombinationEquality(KeyBinding.DEFAULT_DIALOG_ACTION, event)) {
                getDefaultButton().ifPresent(Button::fire);
            }

            // all buttons in base dialogs react on enter
            if (event.getCode() == KeyCode.ENTER) {
                if (event.getTarget() instanceof Button) {
                    ((Button) event.getTarget()).fire();
                    event.consume();
                }
            }
        });

        this.setOnShowing(_ -> applyButtonFix(this.getDialogPane()));

        setDialogIcon(IconTheme.getJabRefImage());
        setResizable(true);
    }

    ///  Applies a fix to prevent truncating ButtonBar buttons with larger font sizes
    private void applyButtonFix(DialogPane pane) {
        // Force the window to fit the new font content bounds
        if (pane.getScene() != null && pane.getScene().getWindow() != null) {
            pane.getScene().getWindow().sizeToScene();
        }

        for (ButtonType type : pane.getButtonTypes()) {
            Node node = pane.lookupButton(type);
            if (node instanceof Button button) {
                // Disabling uniform size prevents the ButtonBar from squeezing
                // buttons into a width that is slightly too small for 10pt or larger text.

                ButtonBar.setButtonUniformSize(button, false);
                button.setMinWidth(Region.USE_PREF_SIZE);
                button.setMaxWidth(Double.MAX_VALUE);

                // Re-trigger CSS to ensure prefWidth is calculated using the new font metrics
                button.applyCss();
            }
        }
    }

    private Optional<Button> getDefaultButton() {
        return Optional.ofNullable((Button) getDialogPane().lookupButton(getDefaultButtonType()));
    }

    private ButtonType getDefaultButtonType() {
        return getDialogPane().getButtonTypes().stream()
                              .filter(buttonType -> buttonType.getButtonData().isDefaultButton())
                              .findFirst()
                              .orElse(ButtonType.OK);
    }

    private void setDialogIcon(Image image) {
        Stage dialogWindow = (Stage) getDialogPane().getScene().getWindow();
        dialogWindow.getIcons().add(image);
    }
}
