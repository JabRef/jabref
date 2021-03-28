package org.jabref.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
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
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.ZipFileChooser;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.jfoenix.controls.JFXSnackbar;
import com.jfoenix.controls.JFXSnackbar.SnackbarEvent;
import com.jfoenix.controls.JFXSnackbarLayout;
import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.TaskProgressView;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.ProgressDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides methods to create default
 * JavaFX dialogs which will also work on top of Swing
 * windows. The created dialogs are instances of the
 * {@link FXDialog} class. The available dialogs in this class
 * are useful for displaying small information graphic dialogs
 * rather than complex windows. For more complex dialogs it is
 * advised to rather create a new sub class of {@link FXDialog}.
 */
public class JabRefDialogService implements DialogService {
    // Snackbar dialog maximum size
    public static final int DIALOG_SIZE_LIMIT = 300;

    private static final Duration TOAST_MESSAGE_DISPLAY_TIME = Duration.millis(3000);
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefDialogService.class);
    private static PreferencesService preferences;

    private final Window mainWindow;
    private final JFXSnackbar statusLine;

    public JabRefDialogService(Window mainWindow, Pane mainPane, PreferencesService preferences) {
        this.mainWindow = mainWindow;
        this.statusLine = new JFXSnackbar(mainPane);
        JabRefDialogService.preferences = preferences;
    }

    private FXDialog createDialog(AlertType type, String title, String content) {
        FXDialog alert = new FXDialog(type, title, true);
        preferences.getTheme().installCss(alert.getDialogPane().getScene());
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.initOwner(mainWindow);
        return alert;
    }

    private FXDialog createDialogWithOptOut(AlertType type, String title, String content,
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
        preferences.getTheme().installCss(alert.getDialogPane().getScene());
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.initOwner(mainWindow);
        return alert;
    }

    public static String shortenDialogMessage(String dialogMessage) {
        if (dialogMessage.length() < JabRefDialogService.DIALOG_SIZE_LIMIT) {
            return dialogMessage.trim();
        }
        return (dialogMessage.substring(0, Math.min(dialogMessage.length(), JabRefDialogService.DIALOG_SIZE_LIMIT)) + "...").trim();
    }

    @Override
    public <T> Optional<T> showChoiceDialogAndWait(String title, String content, String okButtonLabel, T defaultChoice, Collection<T> choices) {
        ChoiceDialog<T> choiceDialog = new ChoiceDialog<>(defaultChoice, choices);
        ((Stage) choiceDialog.getDialogPane().getScene().getWindow()).getIcons().add(IconTheme.getJabRefImage());
        ButtonType okButtonType = new ButtonType(okButtonLabel, ButtonBar.ButtonData.OK_DONE);
        choiceDialog.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, okButtonType);
        choiceDialog.setHeaderText(title);
        choiceDialog.setTitle(title);
        choiceDialog.setContentText(content);
        choiceDialog.initOwner(mainWindow);
        preferences.getTheme().installCss(choiceDialog.getDialogPane().getScene());
        return choiceDialog.showAndWait();
    }

    @Override
    public Optional<String> showInputDialogAndWait(String title, String content) {
        TextInputDialog inputDialog = new TextInputDialog();
        inputDialog.setHeaderText(title);
        inputDialog.setContentText(content);
        inputDialog.initOwner(mainWindow);
        preferences.getTheme().installCss(inputDialog.getDialogPane().getScene());
        return inputDialog.showAndWait();
    }

    @Override
    public Optional<String> showInputDialogWithDefaultAndWait(String title, String content, String defaultValue) {
        TextInputDialog inputDialog = new TextInputDialog(defaultValue);
        inputDialog.setHeaderText(title);
        inputDialog.setContentText(content);
        inputDialog.initOwner(mainWindow);
        preferences.getTheme().installCss(inputDialog.getDialogPane().getScene());
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
        exceptionDialog.getDialogPane().setMaxWidth(mainWindow.getWidth() / 2);
        exceptionDialog.setHeaderText(message);
        exceptionDialog.initOwner(mainWindow);
        preferences.getTheme().installCss(exceptionDialog.getDialogPane().getScene());
        exceptionDialog.showAndWait();
    }

    @Override
    public void showErrorDialogAndWait(String title, String content, Throwable exception) {
        ExceptionDialog exceptionDialog = new ExceptionDialog(exception);
        exceptionDialog.setHeaderText(title);
        exceptionDialog.setContentText(content);
        exceptionDialog.initOwner(mainWindow);
        preferences.getTheme().installCss(exceptionDialog.getDialogPane().getScene());
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
        alert.initOwner(mainWindow);
        preferences.getTheme().installCss(alert.getDialogPane().getScene());
        return alert.showAndWait();
    }

    @Override
    public <R> Optional<R> showCustomDialogAndWait(javafx.scene.control.Dialog<R> dialog) {
        if (dialog.getOwner() == null) {
            dialog.initOwner(mainWindow);
        }
        return dialog.showAndWait();
    }

    @Override
    public <V> void showProgressDialog(String title, String content, Task<V> task) {
        ProgressDialog progressDialog = new ProgressDialog(task);
        progressDialog.setHeaderText(null);
        progressDialog.setTitle(title);
        progressDialog.setContentText(content);
        progressDialog.setGraphic(null);
        ((Stage) progressDialog.getDialogPane().getScene().getWindow()).getIcons().add(IconTheme.getJabRefImage());
        progressDialog.setOnCloseRequest(evt -> task.cancel());
        DialogPane dialogPane = progressDialog.getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.CANCEL);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        cancelButton.setOnAction(evt -> {
            task.cancel();
            progressDialog.close();
        });
        preferences.getTheme().installCss(progressDialog.getDialogPane().getScene());
        progressDialog.initOwner(mainWindow);
        progressDialog.show();
    }

    @Override
    public <V> Optional<ButtonType> showBackgroundProgressDialogAndWait(String title, String content, StateManager stateManager) {
        TaskProgressView<Task<?>> taskProgressView = new TaskProgressView<>();
        EasyBind.bindContent(taskProgressView.getTasks(), stateManager.getBackgroundTasks());
        taskProgressView.setRetainTasks(false);
        taskProgressView.setGraphicFactory(BackgroundTask::getIcon);

        Label message = new Label(content);

        VBox box = new VBox(taskProgressView, message);

        DialogPane contentPane = new DialogPane();
        contentPane.setContent(box);

        FXDialog alert = new FXDialog(AlertType.WARNING, title);
        alert.setDialogPane(contentPane);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.CANCEL);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setResizable(true);
        alert.initOwner(mainWindow);
        preferences.getTheme().installCss(alert.getDialogPane().getScene());

        stateManager.getAnyTaskRunning().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                alert.setResult(ButtonType.YES);
                alert.close();
            }
        });

        return alert.showAndWait();
    }

    @Override
    public void notify(String message) {
        LOGGER.info(message);
        statusLine.fireEvent(new SnackbarEvent(new JFXSnackbarLayout(message), TOAST_MESSAGE_DISPLAY_TIME, null));
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

    @Override
    public Optional<Path> showFileOpenFromArchiveDialog(Path archivePath) throws IOException {
        try (FileSystem zipFile = FileSystems.newFileSystem(archivePath, (ClassLoader) null)) {
            return new ZipFileChooser(zipFile).showAndWait();
        } catch (NoClassDefFoundError exc) {
            throw new IOException("Could not instantiate ZIP-archive reader.", exc);
        }
    }

    @Override
    public void showCustomDialog(BaseDialog<?> aboutDialogView) {
        if (aboutDialogView.getOwner() == null) {
            aboutDialogView.initOwner(mainWindow);
        }
        aboutDialogView.show();
    }
}
