package org.jabref.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.concurrent.Task;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextInputDialog;

import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;

import org.controlsfx.dialog.ProgressDialog;

/**
 * This interface provides methods to create dialogs and show them to the user.
 */
public interface DialogService {

    /**
     * This will create and display new {@link ChoiceDialog} of type T with a default choice and a collection of possible choices
     *
     * @implNote The implementation should accept {@code null} for {@code defaultChoice}, but callers should use {@link #showChoiceDialogAndWait(String, String, String, Collection)}.
     */
    <T> Optional<T> showChoiceDialogAndWait(String title, String content, String okButtonLabel, T defaultChoice, Collection<T> choices);

    /**
     * This will create and display new {@link ChoiceDialog} of type T with a collection of possible choices
     */
    default <T> Optional<T> showChoiceDialogAndWait(String title, String content, String okButtonLabel, Collection<T> choices) {
        return showChoiceDialogAndWait(title, content, okButtonLabel, null, choices);
    }

    /**
     * This will create and display new {@link TextInputDialog} with a text fields to enter data
     */
    Optional<String> showInputDialogAndWait(String title, String content);

    /**
     * This will create and display new {@link TextInputDialog} with a text field with a default value to enter data
     */
    Optional<String> showInputDialogWithDefaultAndWait(String title, String content, String defaultValue);

    /**
     * This will create and display a new information dialog.
     * It will include a blue information icon on the left and
     * a single OK Button. To create an information dialog with custom
     * buttons see also {@link #showCustomButtonDialogAndWait(Alert.AlertType, String, String, ButtonType...)}
     */
    void showInformationDialogAndWait(String title, String content);

    /**
     * This will create and display a new information dialog.
     * It will include a yellow warning icon on the left and
     * a single OK Button. To create a warning dialog with custom
     * buttons see also {@link #showCustomButtonDialogAndWait(Alert.AlertType, String, String, ButtonType...)}
     */
    void showWarningDialogAndWait(String title, String content);

    /**
     * This will create and display a new error dialog.
     * It will include a red error icon on the left and
     * a single OK Button. To create a error dialog with custom
     * buttons see also {@link #showCustomButtonDialogAndWait(Alert.AlertType, String, String, ButtonType...)}
     */
    void showErrorDialogAndWait(String title, String content);

    /**
     * Create and display error dialog displaying the given exception.
     *
     * @param message   the error message
     * @param exception the exception causing the error
     */
    void showErrorDialogAndWait(String message, Throwable exception);

    /**
     * Create and display error dialog displaying the given exception.
     *
     * @param exception the exception causing the error
     */
    default void showErrorDialogAndWait(Exception exception) {
        showErrorDialogAndWait(Localization.lang("Unhandled exception occurred."), exception);
    }

    /**
     * Create and display error dialog displaying the given exception.
     *
     * @param exception the exception causing the error
     */
    void showErrorDialogAndWait(String title, String content, Throwable exception);

    /**
     * Create and display error dialog displaying the given message.
     *
     * @param message the error message
     */
    void showErrorDialogAndWait(String message);

    /**
     * This will create and display a new confirmation dialog.
     * It will include a blue question icon on the left and
     * a OK and Cancel button. To create a confirmation dialog with custom
     * buttons see also {@link #showCustomButtonDialogAndWait(Alert.AlertType, String, String, ButtonType...)}
     *
     * @return true if the use clicked "OK" otherwise false
     */
    boolean showConfirmationDialogAndWait(String title, String content);

    /**
     * Create and display a new confirmation dialog.
     * It will include a blue question icon on the left and
     * a OK (with given label) and Cancel button. To create a confirmation dialog with custom
     * buttons see also {@link #showCustomButtonDialogAndWait(Alert.AlertType, String, String, ButtonType...)}.
     *
     * @return true if the use clicked "OK" otherwise false
     */
    boolean showConfirmationDialogAndWait(String title, String content, String okButtonLabel);

    /**
     * Create and display a new confirmation dialog.
     * It will include a blue question icon on the left and
     * a OK (with given label) and Cancel (also with given label) button. To create a confirmation dialog with custom
     * buttons see also {@link #showCustomButtonDialogAndWait(Alert.AlertType, String, String, ButtonType...)}.
     *
     * @return true if the use clicked "OK" otherwise false
     */
    boolean showConfirmationDialogAndWait(String title, String content, String okButtonLabel, String cancelButtonLabel);

    /**
     * Create and display a new confirmation dialog.
     * It will include a blue question icon on the left and
     * a YES (with given label) and Cancel (also with given label) button. To create a confirmation dialog with custom
     * buttons see also {@link #showCustomButtonDialogAndWait(Alert.AlertType, String, String, ButtonType...)}.
     * Moreover, the dialog contains a opt-out checkbox with the given text to support "Do not ask again"-behaviour.
     *
     * @return true if the use clicked "YES" otherwise false
     */
    boolean showConfirmationDialogWithOptOutAndWait(String title, String content,
                                                    String optOutMessage, Consumer<Boolean> optOutAction);

    /**
     * Create and display a new confirmation dialog.
     * It will include a blue question icon on the left and
     * a YES (with given label) and Cancel (also with given label) button. To create a confirmation dialog with custom
     * buttons see also {@link #showCustomButtonDialogAndWait(Alert.AlertType, String, String, ButtonType...)}.
     * Moreover, the dialog contains a opt-out checkbox with the given text to support "Do not ask again"-behaviour.
     *
     * @return true if the use clicked "YES" otherwise false
     */
    boolean showConfirmationDialogWithOptOutAndWait(String title, String content,
                                                    String okButtonLabel, String cancelButtonLabel,
                                                    String optOutMessage, Consumer<Boolean> optOutAction);

    /**
     * Shows a custom dialog without returning any results.
     *
     * @param dialog dialog to show
     */
    void showCustomDialog(BaseDialog<?> dialog);

    /**
     * This will create and display a new dialog of the specified
     * {@link Alert.AlertType} but with user defined buttons as optional
     * {@link ButtonType}s.
     *
     * @return Optional with the pressed Button as ButtonType
     */
    Optional<ButtonType> showCustomButtonDialogAndWait(Alert.AlertType type, String title, String content,
                                                       ButtonType... buttonTypes);

    /**
     * This will create and display a new dialog showing a custom {@link DialogPane}
     * and using custom {@link ButtonType}s.
     *
     * @return Optional with the pressed Button as ButtonType
     */
    Optional<ButtonType> showCustomDialogAndWait(String title, DialogPane contentPane, ButtonType... buttonTypes);

    /**
     * Shows a custom dialog and returns the result.
     *
     * @param dialog dialog to show
     * @param <R>    type of result
     */
    <R> Optional<R> showCustomDialogAndWait(javafx.scene.control.Dialog<R> dialog);

    /**
     * Constructs and shows a canceable {@link ProgressDialog}. Clicking cancel will cancel the underlying service and close the dialog
     *
     * @param title   title of the dialog
     * @param content message to show above the progress bar
     * @param task    The {@link Task} which executes the work and for which to show the dialog
     */
    <V> void showProgressDialog(String title, String content, Task<V> task);

    /**
     * Constructs and shows a dialog showing the progress of running background tasks.
     * Clicking cancel will cancel the underlying service and close the dialog.
     * The dialog will exit as soon as none of the background tasks are running
     *
     * @param title        title of the dialog
     * @param content      message to show below the list of background tasks
     * @param stateManager The {@link StateManager} which contains the background tasks
     */
    <V> Optional<ButtonType> showBackgroundProgressDialogAndWait(String title, String content, StateManager stateManager);

    /**
     * Notify the user in a non-blocking way (i.e., in form of toast in a snackbar).
     *
     * @param message the message to show.
     */
    void notify(String message);

    /**
     * Shows a new file save dialog. The method doesn't return until the
     * displayed file save dialog is dismissed. The return value specifies the
     * file chosen by the user or an empty {@link Optional} if no selection has been made.
     * After a file was selected, the given file dialog configuration is updated with the selected extension type (if any).
     *
     * @return the selected file or an empty {@link Optional} if no file has been selected
     */
    Optional<Path> showFileSaveDialog(FileDialogConfiguration fileDialogConfiguration);

    /**
     * Shows a new file open dialog. The method doesn't return until the
     * displayed open dialog is dismissed. The return value specifies
     * the file chosen by the user or an empty {@link Optional} if no selection has been
     * made.
     * After a file was selected, the given file dialog configuration is updated with the selected extension type (if any).
     *
     * @return the selected file or an empty {@link Optional} if no file has been selected
     */
    Optional<Path> showFileOpenDialog(FileDialogConfiguration fileDialogConfiguration);

    /**
     * Shows a new file open dialog. The method doesn't return until the
     * displayed open dialog is dismissed. The return value specifies
     * the files chosen by the user or an empty {@link List} if no selection has been
     * made.
     *
     * @return the selected files or an empty {@link List} if no file has been selected
     */
    List<Path> showFileOpenDialogAndGetMultipleFiles(FileDialogConfiguration fileDialogConfiguration);

    /**
     * Shows a new directory selection dialog. The method doesn't return until the
     * displayed open dialog is dismissed. The return value specifies
     * the file chosen by the user or an empty {@link Optional} if no selection has been
     * made.
     *
     * @return the selected directory or an empty {@link Optional} if no directory has been selected
     */
    Optional<Path> showDirectorySelectionDialog(DirectoryDialogConfiguration directoryDialogConfiguration);

    /**
     * Displays a Print Dialog. Allow the user to update job state such as printer and settings. These changes will be
     * available in the appropriate properties after the print dialog has returned. The print dialog is also used to
     * confirm the user wants to proceed with printing.
     *
     * @param job the print job to customize
     * @return false if the user opts to cancel printing
     */
    boolean showPrintDialog(PrinterJob job);

    /**
     * Shows a new dialog that list all files contained in the given archive and which lets the user select one of these
     * files. The method doesn't return until the displayed open dialog is dismissed. The return value specifies the
     * file chosen by the user or an empty {@link Optional} if no selection has been made.
     *
     * @return the selected file or an empty {@link Optional} if no file has been selected
     */
    Optional<Path> showFileOpenFromArchiveDialog(Path archivePath) throws IOException;
}
