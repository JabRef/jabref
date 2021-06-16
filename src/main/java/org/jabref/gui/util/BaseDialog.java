package org.jabref.gui.util;

import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.jabref.gui.Globals;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;

import com.tobiasdiez.easybind.EasyBind;

public class BaseDialog<T> extends Dialog<T> implements org.jabref.gui.Dialog<T> {

    protected BaseDialog() {
        getDialogPane().getScene().setOnKeyPressed(event -> {
            KeyBindingRepository keyBindingRepository = Globals.getKeyPrefs();
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

        setDialogIcon(IconTheme.getJabRefImage());
        setResizable(true);

        EasyBind.wrapNullable(dialogPaneProperty())
                .mapObservable(Node::sceneProperty)
                .subscribeToValues(scene -> Globals.prefs.getTheme().installCss(scene));
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
