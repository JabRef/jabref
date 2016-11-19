package net.sf.jabref.gui;

import java.util.Optional;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

/**
 * This class provides methods to create default
 * JavaFX dialogs which will also work on top of Swing
 * windows. The created dialogs are instances of the
 * {@link FXDialog} class. The available dialogs in this class
 * are useful for displaying small information graphic dialogs
 * rather than complex windows. For more complex dialogs it is
 * advised to rather create a new sub class of {@link FXDialog}.
 */
public class FXDialogService implements DialogService {

    private static FXDialog createDialog(AlertType type, String title, String content) {
        FXDialog alert = new FXDialog(type, title, true);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert;
    }

    @Override
    public void showInformationDialogAndWait(String title, String content) {
        FXDialog alert = createDialog(AlertType.INFORMATION, title, content);
        alert.showAndWait();
    }

    @Override
    public void showWarningDialogAndWait(String title, String content) {
        FXDialog alert = createDialog(AlertType.WARNING, title, content);
        alert.showAndWait();
    }

    @Override
    public void showErrorDialogAndWait(String title, String content) {
        FXDialog alert = createDialog(AlertType.ERROR, title, content);
        alert.showAndWait();
    }

    @Override
    public Optional<ButtonType> showConfirmationDialogAndWait(String title, String content) {
        FXDialog alert = createDialog(AlertType.CONFIRMATION, title, content);
        return alert.showAndWait();
    }

    @Override
    public Optional<ButtonType> showCustomButtonDialogAndWait(AlertType type, String title, String content,
            ButtonType... buttonTypes) {
        FXDialog alert = createDialog(type, title, content);
        alert.getButtonTypes().setAll(buttonTypes);
        return alert.showAndWait();
    }

    @Override
    public Optional<ButtonType> showCustomDialogAndWait(String title, DialogPane contentPane,
            ButtonType... buttonTypes) {
        FXDialog alert = new FXDialog(AlertType.NONE, title);
        alert.setDialogPane(contentPane);
        alert.getButtonTypes().setAll(buttonTypes);
        return alert.showAndWait();
    }
}
