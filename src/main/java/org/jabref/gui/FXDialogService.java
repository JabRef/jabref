package org.jabref.gui;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import org.jabref.JabRefGUI;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;

import org.controlsfx.dialog.ExceptionDialog;

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
    public void showErrorDialogAndWait(String message, Throwable exception) {
        ExceptionDialog exceptionDialog = new ExceptionDialog(exception);
        exceptionDialog.setHeaderText(message);
        exceptionDialog.showAndWait();
    }

    @Override
    public void showErrorDialogAndWait(String message) {
        FXDialog alert = createDialog(AlertType.ERROR, Localization.lang("Error Occurred"), message);
        alert.showAndWait();
    }

    @Override
    public boolean showConfirmationDialogAndWait(String title, String content) {
        FXDialog alert = createDialog(AlertType.CONFIRMATION, title, content);
        return alert.showAndWait().filter(buttonType -> buttonType == ButtonType.OK).isPresent();
    }

    @Override
    public boolean showConfirmationDialogAndWait(String title, String content, String okButtonLabel) {
        FXDialog alert = createDialog(AlertType.CONFIRMATION, title, content);
        ButtonType okButtonType = new ButtonType(okButtonLabel, ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(ButtonType.CANCEL, okButtonType);
        return alert.showAndWait().filter(buttonType -> buttonType == okButtonType).isPresent();
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

    @Override
    public <R> Optional<R> showCustomDialogAndWait(Dialog<R> dialog) {
        return dialog.showAndWait();
    }

    @Override
    public void notify(String message) {
        JabRefGUI.getMainFrame().output(message);
    }

    @Override
    public Optional<Path> showFileSaveDialog(FileDialogConfiguration fileDialogConfiguration) {
        FileChooser chooser = getConfiguredFileChooser(fileDialogConfiguration);
        File file = chooser.showSaveDialog(null);
        return Optional.ofNullable(file).map(File::toPath);
    }

    @Override
    public Optional<Path> showFileOpenDialog(FileDialogConfiguration fileDialogConfiguration) {
        FileChooser chooser = getConfiguredFileChooser(fileDialogConfiguration);
        File file = chooser.showOpenDialog(null);
        return Optional.ofNullable(file).map(File::toPath);
    }

    @Override
    public Optional<Path> showDirectorySelectionDialog(DirectoryDialogConfiguration directoryDialogConfiguration) {
        DirectoryChooser chooser = getConfiguredDirectoryChooser(directoryDialogConfiguration);
        File file = chooser.showDialog(null);
        return Optional.ofNullable(file).map(File::toPath);
    }

    private DirectoryChooser getConfiguredDirectoryChooser(DirectoryDialogConfiguration directoryDialogConfiguration) {
        DirectoryChooser chooser = new DirectoryChooser();
        directoryDialogConfiguration.getInitialDirectory().map(Path::toFile).ifPresent(chooser::setInitialDirectory);
        return chooser;
    }

    private FileChooser getConfiguredFileChooser(FileDialogConfiguration fileDialogConfiguration) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(fileDialogConfiguration.getExtensionFilters());
        chooser.setSelectedExtensionFilter(fileDialogConfiguration.getDefaultExtension());
        chooser.setInitialFileName(fileDialogConfiguration.getInitialFileName());
        fileDialogConfiguration.getInitialDirectory().map(Path::toFile).ifPresent(chooser::setInitialDirectory);
        return chooser;
    }
}
