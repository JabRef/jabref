package org.jabref.gui;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.concurrent.Task;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import org.jabref.JabRefGUI;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;

import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.ProgressDialog;

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

    private final Window mainWindow;

    /**
     * @deprecated try not to initialize a new dialog service but reuse the one constructed in {@link org.jabref.gui.JabRefFrame}.
     */
    @Deprecated
    public FXDialogService() {
        this(null);
    }

    public FXDialogService(Window mainWindow) {
        this.mainWindow = mainWindow;
    }

    private static FXDialog createDialog(AlertType type, String title, String content) {
        FXDialog alert = new FXDialog(type, title, true);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
        return alert;
    }

    private static FXDialog createDialogWithOptOut(AlertType type, String title, String content,
            String optOutMessage, Consumer<Boolean> optOutAction) {
        FXDialog alert = new FXDialog(type, title, true);
        // Need to force the alert to layout in order to grab the graphic as we are replacing the dialog pane with a custom pane
        alert.getDialogPane().applyCss();
        Node graphic = alert.getDialogPane().getGraphic();

        // Create a new dialog pane that has a checkbox instead of the hide/show details button
        // Use the supplied callback for the action of the checkbox
        alert.setDialogPane(new DialogPane() {

            @Override
            protected Node createDetailsButton() {
                CheckBox optOut = new CheckBox();
                optOut.setText(optOutMessage);
                optOut.setOnAction(e -> optOutAction.accept(optOut.isSelected()));
                return optOut;
            }
        });

        // Fool the dialog into thinking there is some expandable content; a group won't take up any space if it has no children
        alert.getDialogPane().setExpandableContent(new Group());
        alert.getDialogPane().setExpanded(true);

        // Reset the dialog graphic using the default style
        alert.getDialogPane().setGraphic(graphic);

        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        return alert;
    }

    @Override
    public <T> Optional<T> showChoiceDialogAndWait(String title, String content, String okButtonLabel, T defaultChoice, Collection<T> choices) {
        ChoiceDialog<T> choiceDialog = new ChoiceDialog<>(defaultChoice, choices);
        ButtonType okButtonType = new ButtonType(okButtonLabel, ButtonBar.ButtonData.OK_DONE);
        choiceDialog.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, okButtonType);
        choiceDialog.setHeaderText(title);
        choiceDialog.setContentText(content);
        return choiceDialog.showAndWait();

    }

    @Override
    public Optional<String> showInputDialogAndWait(String title, String content) {
        TextInputDialog inputDialog = new TextInputDialog();
        inputDialog.setHeaderText(title);
        inputDialog.setContentText(content);
        return inputDialog.showAndWait();
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
    public void showErrorDialogAndWait(String title, String content, Throwable exception) {
        ExceptionDialog exceptionDialog = new ExceptionDialog(exception);
        exceptionDialog.setHeaderText(title);
        exceptionDialog.setContentText(content);
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
    public boolean showConfirmationDialogAndWait(String title, String content,
            String okButtonLabel, String cancelButtonLabel) {
        FXDialog alert = createDialog(AlertType.CONFIRMATION, title, content);
        ButtonType okButtonType = new ButtonType(okButtonLabel, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(cancelButtonLabel, ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(okButtonType, cancelButtonType);
        return alert.showAndWait().filter(buttonType -> buttonType == okButtonType).isPresent();
    }

    @Override
    public boolean showConfirmationDialogWithOptOutAndWait(String title, String content,
            String optOutMessage, Consumer<Boolean> optOutAction) {
        FXDialog alert = createDialogWithOptOut(AlertType.CONFIRMATION, title, content, optOutMessage, optOutAction);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        return alert.showAndWait().filter(buttonType -> buttonType == ButtonType.YES).isPresent();
    }

    @Override
    public boolean showConfirmationDialogWithOptOutAndWait(String title, String content,
            String okButtonLabel, String cancelButtonLabel,
            String optOutMessage, Consumer<Boolean> optOutAction) {
        FXDialog alert = createDialogWithOptOut(AlertType.CONFIRMATION, title, content, optOutMessage, optOutAction);
        ButtonType okButtonType = new ButtonType(okButtonLabel, ButtonBar.ButtonData.YES);
        ButtonType cancelButtonType = new ButtonType(cancelButtonLabel, ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(okButtonType, cancelButtonType);
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
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setResizable(true);
        return alert.showAndWait();
    }

    @Override
    public <R> Optional<R> showCustomDialogAndWait(Dialog<R> dialog) {
        return dialog.showAndWait();
    }

    @Override
    public <V> void showCanceableProgressDialogAndWait(Task<V> task) {
        ProgressDialog progressDialog = new ProgressDialog(task);
        progressDialog.setOnCloseRequest(evt -> task.cancel());
        DialogPane dialogPane = progressDialog.getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.CANCEL);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        cancelButton.setOnAction(evt -> {
            task.cancel();
            progressDialog.close();
        });
        progressDialog.show();
    }

    @Override
    public void notify(String message) {
        JabRefGUI.getMainFrame().output(message);
    }

    @Override
    public Optional<Path> showFileSaveDialog(FileDialogConfiguration fileDialogConfiguration) {
        FileChooser chooser = getConfiguredFileChooser(fileDialogConfiguration);
        File file = chooser.showSaveDialog(mainWindow);
        Optional.ofNullable(chooser.getSelectedExtensionFilter()).ifPresent(fileDialogConfiguration::setSelectedExtensionFilter);
        return Optional.ofNullable(file).map(File::toPath);
    }

    @Override
    public Optional<Path> showFileOpenDialog(FileDialogConfiguration fileDialogConfiguration) {
        FileChooser chooser = getConfiguredFileChooser(fileDialogConfiguration);
        File file = chooser.showOpenDialog(mainWindow);
        Optional.ofNullable(chooser.getSelectedExtensionFilter()).ifPresent(fileDialogConfiguration::setSelectedExtensionFilter);
        return Optional.ofNullable(file).map(File::toPath);
    }

    @Override
    public Optional<Path> showDirectorySelectionDialog(DirectoryDialogConfiguration directoryDialogConfiguration) {
        DirectoryChooser chooser = getConfiguredDirectoryChooser(directoryDialogConfiguration);
        File file = chooser.showDialog(mainWindow);
        return Optional.ofNullable(file).map(File::toPath);
    }

    @Override
    public List<Path> showFileOpenDialogAndGetMultipleFiles(FileDialogConfiguration fileDialogConfiguration) {
        FileChooser chooser = getConfiguredFileChooser(fileDialogConfiguration);
        List<File> files = chooser.showOpenMultipleDialog(mainWindow);
        return files != null ? files.stream().map(File::toPath).collect(Collectors.toList()) : Collections.emptyList();
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

    @Override
    public boolean showPrintDialog(PrinterJob job) {
        return job.showPrintDialog(mainWindow);
    }
}
