package org.jabref.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.application.Platform;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.BaseWindow;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.gui.util.ZipFileChooser;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.http.SimpleHttpResponse;

import com.dlsc.gemsfx.infocenter.Notification;
import com.dlsc.gemsfx.infocenter.NotificationGroup;
import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.TaskProgressView;
import org.controlsfx.control.textfield.CustomPasswordField;
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

    private final NotificationGroup<Object, FileNotification> fileNotifications = new NotificationGroup<>("Files");
    private final NotificationGroup<Object, PreviewNotification> previewNotifications = new NotificationGroup<>("Preview");
    private final NotificationGroup<Object, Notification<Object>> undefinedNotifications = new NotificationGroup<>("Notifications");

    private final Window mainWindow;

    public JabRefDialogService(Window mainWindow) {
        this.mainWindow = mainWindow;
    }

    private FXDialog createDialog(AlertType type, String title, String content) {
        FXDialog alert = new FXDialog(type, title, true);
        alert.setHeaderText(null);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setResizable(true);

        TextArea area = new TextArea(content);
        area.setWrapText(true);

        alert.getDialogPane().setContent(area);
        alert.initOwner(mainWindow);
        return alert;
    }

    private FXDialog createDialogWithOptOut(String title, String content,
                                            String optOutMessage, Consumer<Boolean> optOutAction) {
        FXDialog alert = new FXDialog(AlertType.CONFIRMATION, title, true);
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
        alert.initOwner(mainWindow);
        return alert;
    }

    public static String shortenDialogMessage(String dialogMessage) {
        if (dialogMessage.length() < JabRefDialogService.DIALOG_SIZE_LIMIT) {
            return dialogMessage.trim();
        }
        return (dialogMessage.substring(0, JabRefDialogService.DIALOG_SIZE_LIMIT) + "...").trim();
    }

    private <T> ChoiceDialog<T> createChoiceDialog(String title, String content, String okButtonLabel, T defaultChoice, Collection<T> choices) {
        ChoiceDialog<T> choiceDialog = new ChoiceDialog<>(defaultChoice, choices);
        ((Stage) choiceDialog.getDialogPane().getScene().getWindow()).getIcons().add(IconTheme.getJabRefImage());
        ButtonType okButtonType = new ButtonType(okButtonLabel, ButtonBar.ButtonData.OK_DONE);
        choiceDialog.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, okButtonType);
        choiceDialog.setHeaderText(title);
        choiceDialog.setTitle(title);
        choiceDialog.setContentText(content);
        choiceDialog.initOwner(mainWindow);
        return choiceDialog;
    }

    @Override
    public <T> Optional<T> showChoiceDialogAndWait(String title, String content, String okButtonLabel, T defaultChoice, Collection<T> choices) {
        return createChoiceDialog(title, content, okButtonLabel, defaultChoice, choices).showAndWait();
    }

    @Override
    public <T> Optional<T> showEditableChoiceDialogAndWait(String title, String content, String okButtonLabel, T defaultChoice, Collection<T> choices, StringConverter<T> converter) {
        ChoiceDialog<T> choiceDialog = createChoiceDialog(title, content, okButtonLabel, defaultChoice, choices);
        ComboBox<T> comboBox = (ComboBox<T>) choiceDialog.getDialogPane().lookup(".combo-box");
        comboBox.setEditable(true);
        comboBox.setConverter(converter);
        EasyBind.subscribe(comboBox.getEditor().textProperty(), text -> comboBox.setValue(converter.fromString(text)));
        return choiceDialog.showAndWait();
    }

    @Override
    public Optional<String> showInputDialogAndWait(String title, String content) {
        TextInputDialog inputDialog = new TextInputDialog();
        inputDialog.setHeaderText(title);
        inputDialog.setContentText(content);
        inputDialog.initOwner(mainWindow);
        return inputDialog.showAndWait();
    }

    @Override
    public Optional<String> showInputDialogWithDefaultAndWait(String title, String content, String defaultValue) {
        TextInputDialog inputDialog = new TextInputDialog(defaultValue);
        inputDialog.setHeaderText(title);
        inputDialog.setContentText(content);
        inputDialog.initOwner(mainWindow);
        inputDialog.getDialogPane().setPrefSize(500, 200);
        inputDialog.setResizable(true);
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
        exceptionDialog.showAndWait();
    }

    @Override
    public void showErrorDialogAndWait(Exception exception) {
        if (exception instanceof FetcherException fetcherException) {
            // Somehow, Java does not route correctly to the other method
            showErrorDialogAndWait(fetcherException);
        } else {
            showErrorDialogAndWait(Localization.lang("Unhandled exception occurred."), exception);
        }
    }

    @Override
    public void showErrorDialogAndWait(FetcherException fetcherException) {
        String failedTitle = Localization.lang("Failed to download from URL");
        String localizedMessage = fetcherException.getLocalizedMessage();
        Optional<SimpleHttpResponse> httpResponse = fetcherException.getHttpResponse();
        if (httpResponse.isPresent()) {
            this.showInformationDialogAndWait(failedTitle, getContentByCode(httpResponse.get().statusCode()) + "\n\n" + localizedMessage);
        } else if (fetcherException instanceof FetcherClientException) {
            this.showErrorDialogAndWait(failedTitle, Localization.lang("Something is wrong on JabRef side. Please check the URL and try again.") + "\n\n" + localizedMessage);
        } else if (fetcherException instanceof FetcherServerException) {
            this.showInformationDialogAndWait(failedTitle,
                    Localization.lang("Error downloading from URL. Cause is likely the server side.\nPlease try again later or contact the server administrator.") + "\n\n" + localizedMessage);
        } else {
            this.showErrorDialogAndWait(failedTitle, localizedMessage);
        }
    }

    @Override
    public void showErrorDialogAndWait(String title, String content, Throwable exception) {
        ExceptionDialog exceptionDialog = new ExceptionDialog(exception);
        exceptionDialog.setHeaderText(title);
        exceptionDialog.setContentText(content);
        exceptionDialog.initOwner(mainWindow);
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
        FXDialog alert = createDialogWithOptOut(title, content, optOutMessage, optOutAction);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        return alert.showAndWait().filter(buttonType -> buttonType == ButtonType.YES).isPresent();
    }

    @Override
    public boolean showConfirmationDialogWithOptOutAndWait(String title, String content,
                                                           String okButtonLabel, String cancelButtonLabel,
                                                           String optOutMessage, Consumer<Boolean> optOutAction) {
        FXDialog alert = createDialogWithOptOut(title, content, optOutMessage, optOutAction);
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
    public Optional<ButtonType> showCustomButtonDialogWithTooltipsAndWait(AlertType type,
                                                                          String title,
                                                                          String content,
                                                                          Map<ButtonType, String> tooltips,
                                                                          ButtonType... buttonTypes) {
        // Use a non-editable Label instead of raw text to prevent editing
        FXDialog alert = createDialog(type, title, "");
        Label contentLabel = new Label(content);
        contentLabel.setWrapText(true);
        alert.getDialogPane().setContent(contentLabel);

        alert.getDialogPane().getButtonTypes().setAll(buttonTypes);

        // Attach tooltips to buttons
        Platform.runLater(() -> {
            for (Map.Entry<ButtonType, String> entry : tooltips.entrySet()) {
                Button button = (Button) alert.getDialogPane().lookupButton(entry.getKey());
                if (button != null) {
                    button.setTooltip(new Tooltip(entry.getValue()));
                }
            }
        });

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
        return alert.showAndWait();
    }

    @Override
    public <R> Optional<R> showCustomDialogAndWait(Dialog<R> dialog) {
        if (dialog.getOwner() == null) {
            dialog.initOwner(mainWindow);
        }
        return dialog.showAndWait();
    }

    @Override
    public Optional<String> showPasswordDialogAndWait(String title, String header, String content) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        CustomPasswordField passwordField = new CustomPasswordField();

        HBox box = new HBox();
        box.setSpacing(10);
        box.getChildren().addAll(new Label(content), passwordField);
        dialog.setTitle(title);
        dialog.getDialogPane().setContent(box);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return passwordField.getText();
            }
            return null;
        });
        return dialog.showAndWait();
    }

    private <V> ProgressDialog createProgressDialog(String title, String content, Task<V> task) {
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
        progressDialog.initOwner(mainWindow);
        return progressDialog;
    }

    @Override
    public <V> void showProgressDialog(String title, String content, Task<V> task) {
        ProgressDialog progressDialog = createProgressDialog(title, content, task);
        progressDialog.show();
    }

    @Override
    public <V> void showProgressDialogAndWait(String title, String content, Task<V> task) {
        ProgressDialog progressDialog = createProgressDialog(title, content, task);
        progressDialog.showAndWait();
    }

    @Override
    public Optional<ButtonType> showBackgroundProgressDialogAndWait(String title, String content, StateManager stateManager) {
        TaskProgressView<Task<?>> taskProgressView = new TaskProgressView<>();
        EasyBind.bindContent(taskProgressView.getTasks(), stateManager.getRunningBackgroundTasks());
        taskProgressView.setRetainTasks(false);
        taskProgressView.setGraphicFactory(task -> ThemeManager.getDownloadIconTitleMap.getOrDefault(task.getTitle(), null));

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

        stateManager.getAnyTasksThatWillNotBeRecoveredRunning().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                alert.setResult(ButtonType.YES);
                alert.close();
            }
        });

        return alert.showAndWait();
    }

    @Override
    public void notify(String message) {
        // TODO: Change to a notification overview instead of event log when that is available.
        //       The event log is not that user friendly (different purpose).
        LOGGER.debug(message);
        UiTaskExecutor.runInJavaFXThread(() -> notify(new UndefinedNotification("Info", message)));
    }

    @Override
    public void notify(Notification<Object> notification) {
        if (notification instanceof FileNotification) {
            fileNotifications.getNotifications().add((FileNotification) notification);
        } else if (notification instanceof PreviewNotification) {
            previewNotifications.getNotifications().add((PreviewNotification) notification);
        } else {
            undefinedNotifications.getNotifications().add(notification);
        }
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
        Optional.ofNullable(chooser.getSelectedExtensionFilter()).ifPresent(fileDialogConfiguration::setSelectedExtensionFilter);
        return files != null ? files.stream().map(File::toPath).toList() : List.of();
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
    public void showCustomDialog(BaseDialog<?> dialogView) {
        if (dialogView.getOwner() == null) {
            dialogView.initOwner(mainWindow);
        }
        dialogView.show();
    }

    @Override
    public void showCustomWindow(BaseWindow window) {
        if (window.getOwner() == null) {
            window.initOwner(mainWindow);
        }
        window.applyStylesheets(mainWindow.getScene().getStylesheets());
        window.show();
    }

    private String getContentByCode(int statusCode) {
        return switch (statusCode) {
            case 401 ->
                    Localization.lang("Access denied. You are not authorized to access this resource. Please check your credentials and try again. If you believe you should have access, please contact the administrator for assistance.");
            case 403 ->
                    Localization.lang("Access denied. You do not have permission to access this resource. Please contact the administrator for assistance or try a different action.");
            case 404 ->
                    Localization.lang("The requested resource could not be found. It seems that the file you are trying to download is not available or has been moved. Please verify the URL and try again. If you believe this is an error, please contact the administrator for further assistance.");
            default ->
                    Localization.lang("Something is wrong on JabRef side. Please check the URL and try again.");
        };
    }

    public static class FileNotification extends Notification<Object> {
        public FileNotification(String title, String description) {
            super(title, description);
            setOnClick(_ -> OnClickBehaviour.HIDE_AND_REMOVE);
        }
    }

    public static class UndefinedNotification extends Notification<Object> {
        public UndefinedNotification(String title, String description) {
            super(title, description);
            setOnClick(_ -> OnClickBehaviour.HIDE_AND_REMOVE);
        }
    }

    public static class PreviewNotification extends Notification<Object> {
        public PreviewNotification(String title, String description) {
            super(title, description);
            setOnClick(_ -> OnClickBehaviour.HIDE_AND_REMOVE);
        }
    }

    public List<NotificationGroup<?, ? extends Notification<Object>>> getNotifications() {
        return List.of(fileNotifications, undefinedNotifications, previewNotifications);
    }
}
