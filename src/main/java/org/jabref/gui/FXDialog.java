package org.jabref.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;

/**
 * This class provides a super class for all dialogs implemented in JavaFX.
 * <p>
 * To create a custom JavaFX dialog one should create an instance of this class and set a dialog
 * pane through the inherited {@link Dialog#setDialogPane(DialogPane)} method.
 * The dialog can be shown via {@link Dialog#show()} or {@link Dialog#showAndWait()}.
 * <p>
 * The layout of the pane should be defined in an external fxml file and loaded it via the
 * {@link FXMLLoader}.
 */
public class FXDialog extends Alert {

    public FXDialog(AlertType type, String title, Image image, boolean isModal) {
        this(type, title, isModal);
        setDialogIcon(image);
    }

    public FXDialog(AlertType type, String title, Image image) {
        this(type, title, true);
        setDialogIcon(image);
    }

    public FXDialog(AlertType type, String title, boolean isModal) {
        this(type, isModal);
        setTitle(title);
    }

    public FXDialog(AlertType type, String title) {
        this(type);
        setTitle(title);
    }

    public FXDialog(AlertType type, boolean isModal) {
        super(type);

        setDialogIcon(IconTheme.getJabRefImage());

        Stage dialogWindow = getDialogWindow();
        dialogWindow.setOnCloseRequest(evt -> this.close());
        if (isModal) {
            initModality(Modality.APPLICATION_MODAL);
        } else {
            initModality(Modality.NONE);
        }

        dialogWindow.getScene().setOnKeyPressed(event -> {
            KeyBindingRepository keyBindingRepository = Globals.getKeyPrefs();
            if (keyBindingRepository.checkKeyCombinationEquality(KeyBinding.CLOSE, event)) {
                dialogWindow.close();
            }
        });
    }

    public FXDialog(AlertType type) {
        this(type, true);
    }

    private void setDialogIcon(Image image) {
        Stage fxDialogWindow = getDialogWindow();
        fxDialogWindow.getIcons().add(image);
    }

    private Stage getDialogWindow() {
        return (Stage) getDialogPane().getScene().getWindow();
    }
}
