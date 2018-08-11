package org.jabref.gui.exporter;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.Optional;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.SidePaneType;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.collab.ChangeScanner;
import org.jabref.gui.dialogs.AutosaveUIManager;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.autosaveandbackup.AutosaveManager;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.FileSaveSession;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.SaveSession;
import org.jabref.logic.l10n.Encodings;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileBasedLock;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.ChangePropagation;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for the "Save" and "Save as" operations called from BasePanel. This class is also used for
 * save operations when closing a database or quitting the applications.
 *
 * The save operation is loaded off of the GUI thread using {@link BackgroundTask}.
 * Callers can query whether the operation was canceled, or whether it was successful.
 */
public class SaveDatabaseAction implements BaseAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveDatabaseAction.class);

    private final BasePanel panel;
    private final JabRefFrame frame;
    private boolean success;
    private boolean canceled;
    private boolean fileLockedError;

    private Optional<Path> filePath;

    public SaveDatabaseAction(BasePanel panel) {
        this.panel = panel;
        this.frame = panel.frame();
        this.filePath = Optional.empty();
    }

    /**
     * @param panel BasePanel which contains the database to be saved
     * @param filePath Path to the file the database should be saved to
     */
    public SaveDatabaseAction(BasePanel panel, Path filePath) {
        this(panel);
        this.filePath = Optional.ofNullable(filePath);
    }

    public void init() throws Exception {
        success = false;
        canceled = false;
        fileLockedError = false;
        if (panel.getBibDatabaseContext().getDatabaseFile().isPresent()) {
            // Check for external modifications: if true, save not performed so do not tell the user a save is underway but return instead.
            if (checkExternalModification()) {
                return;
            }

            panel.frame().output(Localization.lang("Saving library") + "...");
            panel.setSaving(true);
        } else if (filePath.isPresent()) {
            // save as directly if the target file location is known
            saveAs(filePath.get().toFile());
        } else {
            saveAs();
        }
    }

    private void doSave() {
        if (canceled || !panel.getBibDatabaseContext().getDatabaseFile().isPresent()) {
            return;
        }

        try {
            // If set in preferences, generate missing BibTeX keys
            panel.autoGenerateKeysBeforeSaving();

            if (FileBasedLock.waitForFileLock(panel.getBibDatabaseContext().getDatabaseFile().get().toPath())) {
                // Check for external modifications to alleviate multiuser concurrency issue when near
                // simultaneous saves occur to a shared database file: if true, do not perform the save
                // rather return instead.
                if (checkExternalModification()) {
                    return;
                }

                // Save the database
                success = saveDatabase(panel.getBibDatabaseContext().getDatabaseFile().get(), false,
                        panel.getBibDatabaseContext()
                             .getMetaData()
                             .getEncoding()
                             .orElse(Globals.prefs.getDefaultEncoding()));

                panel.updateTimeStamp();
            } else {
                success = false;
                fileLockedError = true;
            }

            // release panel from save status
            panel.setSaving(false);

            if (success) {
                panel.getUndoManager().markUnchanged();
                // (Only) after a successful save the following
                // statement marks that the base is unchanged
                // since last save:
                panel.setNonUndoableChange(false);
                panel.setBaseChanged(false);
                panel.markExternalChangesAsResolved();
            }
        } catch (SaveException ex) {
            if (ex == SaveException.FILE_LOCKED) {
                success = false;
                fileLockedError = true;
                return;
            }
            LOGGER.error("Problem saving file", ex);
        }

        if (success) {
            DefaultTaskExecutor.runInJavaFXThread(() -> {
                // Reset title of tab
                frame.setTabTitle(panel, panel.getTabTitle(),
                        panel.getBibDatabaseContext().getDatabaseFile().get().getAbsolutePath());
                frame.output(Localization.lang("Saved library") + " '"
                        + panel.getBibDatabaseContext().getDatabaseFile().get().getPath() + "'.");
                frame.setWindowTitle();
                frame.updateAllTabTitles();
            });
        } else if (!canceled) {
            if (fileLockedError) {
                // TODO: user should have the option to override the lock file.
                frame.output(Localization.lang("Could not save, file locked by another JabRef instance."));
            } else {
                frame.output(Localization.lang("Save failed"));
            }
        }
    }

    private boolean saveDatabase(File file, boolean selectedOnly, Charset encoding) throws SaveException {
        SaveSession session;

        try {
            SavePreferences prefs = Globals.prefs.loadForSaveFromPreferences().withEncoding(encoding);
            BibtexDatabaseWriter<SaveSession> databaseWriter = new BibtexDatabaseWriter<>(FileSaveSession::new);

            if (selectedOnly) {
                session = databaseWriter.savePartOfDatabase(panel.getBibDatabaseContext(), panel.getSelectedEntries(),
                        prefs);
            } else {
                session = databaseWriter.saveDatabase(panel.getBibDatabaseContext(), prefs);
            }

            panel.registerUndoableChanges(session);

        } catch (UnsupportedCharsetException ex) {
            frame.getDialogService().showErrorDialogAndWait(Localization.lang("Save library"), Localization.lang("Could not save file.")
                    + Localization.lang("Character encoding '%0' is not supported.", encoding.displayName()));
            throw new SaveException(ex.getMessage(), ex);
        } catch (SaveException ex) {
            if (ex == SaveException.FILE_LOCKED) {
                throw ex;
            }
            if (ex.specificEntry()) {
                BibEntry entry = ex.getEntry();
                // Error occured during processing of an entry. Highlight it!
                panel.clearAndSelect(entry);
            } else {
                LOGGER.error("A problem occured when trying to save the file", ex);
            }

            frame.getDialogService().showErrorDialogAndWait(Localization.lang("Save library"), Localization.lang("Could not save file."), ex);
            throw ex;
        }

        // handle encoding problems
        boolean success = true;
        if (!session.getWriter().couldEncodeAll()) {

            DialogPane pane = new DialogPane();
            TextArea area = new TextArea(session.getWriter().getProblemCharacters());
            VBox vbox = new VBox();
            vbox.getChildren().addAll(
                    new Text(Localization.lang("The chosen encoding '%0' could not encode the following characters:",
                            session.getEncoding().displayName())),
                    area,
                    new Text(Localization.lang("What do you want to do?"))

            );
            pane.setContent(vbox);

            ButtonType tryDiff = new ButtonType(Localization.lang("Try different encoding"), ButtonData.OTHER);
            ButtonType save = new ButtonType(Localization.lang("Save"), ButtonData.APPLY);

            Optional<ButtonType> clickedBtn = frame.getDialogService().showCustomDialogAndWait(Localization.lang("Save library"), pane, save, tryDiff, ButtonType.CANCEL);

            if (clickedBtn.isPresent() && clickedBtn.get().equals(tryDiff)) {
                Optional<Charset> selectedCharSet = frame.getDialogService().showChoiceDialogAndWait(Localization.lang("Save library"), Localization.lang("Select encoding"), Localization.lang("Save library"), encoding, Encodings.getCharsets());

                if (selectedCharSet.isPresent()) {

                    Charset newEncoding = selectedCharSet.get();
                    return saveDatabase(file, selectedOnly, newEncoding);
                } else {
                    success = false;
                }
            }
        }

        // backup file?
        try {
            if (success) {
                session.commit(file.toPath());
                // Make sure to remember which encoding we used.
                panel.getBibDatabaseContext().getMetaData().setEncoding(encoding, ChangePropagation.DO_NOT_POST_EVENT);
            } else {
                session.cancel();
            }
        } catch (SaveException e) {
            LOGGER.debug("Problems saving during backup creationg", e);
            boolean saveWithoutBackupClicked = frame.getDialogService().showConfirmationDialogAndWait(Localization.lang("Unable to create backup"),
                    Localization.lang("Save failed during backup creation") + ". " + Localization.lang("Save without backup?"),
                    Localization.lang("Save without backup"), Localization.lang("Cancel"));

            if (saveWithoutBackupClicked) {
                session.setUseBackup(false);

                session.commit(file.toPath());
                panel.getBibDatabaseContext().getMetaData().setEncoding(encoding, ChangePropagation.DO_NOT_POST_EVENT);
            } else {
                success = false;
            }
        }

        return success;
    }

    /**
     * Run the "Save" operation. This method offloads the actual save operation to a background thread.
     */
    public void runCommand() throws Exception {
        action();
    }

    public void save() throws Exception {
        runCommand();
    }

    public void saveAs() throws Exception {
        // configure file dialog

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.BIBTEX_DB)
                .withDefaultExtension(StandardFileType.BIBTEX_DB)
                .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY))
                .build();
        DialogService dialogService = frame.getDialogService();
        Optional<Path> path = dialogService.showFileSaveDialog(fileDialogConfiguration);
        if (path.isPresent()) {
            saveAs(path.get().toFile());
        } else {
            canceled = true;
            return;
        }
    }

    /**
     * Run the "Save as" operation. This method offloads the actual save operation to a background thread.
     */
    public void saveAs(File file) throws Exception {
        BibDatabaseContext context = panel.getBibDatabaseContext();

        if (context.getLocation() == DatabaseLocation.SHARED) {
            // Save all properties dependent on the ID. This makes it possible to restore them.
            new SharedDatabasePreferences(context.getDatabase().generateSharedDatabaseID())
                    .putAllDBMSConnectionProperties(context.getDBMSSynchronizer().getConnectionProperties());

        }

        context.setDatabaseFile(file);
        if (file.getParent() != null) {
            Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, file.getParent());
        }
        runCommand();
        // If the operation failed, revert the file field and return:
        if (!success) {
            return;
        }

        Optional<Path> databasePath = context.getDatabasePath();
        if (databasePath.isPresent()) {
            final Path oldFile = databasePath.get();
            context.setDatabaseFile(oldFile.toFile());
            //closing AutosaveManager and BackupManager for original library
            AutosaveManager.shutdown(context);
            BackupManager.shutdown(context);
        } else {
            LOGGER.info("Old file not found, just creating a new file");
        }
        context.setDatabaseFile(file);
        panel.resetChangeMonitor();

        if (readyForAutosave(context)) {
            AutosaveManager autosaver = AutosaveManager.start(context);
            autosaver.registerListener(new AutosaveUIManager(panel));
        }

        if (readyForBackup(context)) {
            BackupManager.start(context);
        }

        context.getDatabaseFile().ifPresent(presentFile -> frame.getFileHistory().newFile(presentFile.getPath()));
    }

    private boolean readyForAutosave(BibDatabaseContext context) {
        return ((context.getLocation() == DatabaseLocation.SHARED) ||
                ((context.getLocation() == DatabaseLocation.LOCAL)
                        && Globals.prefs.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE)))
                &&
                context.getDatabaseFile().isPresent();
    }

    private boolean readyForBackup(BibDatabaseContext context) {
        return (context.getLocation() == DatabaseLocation.LOCAL) && context.getDatabaseFile().isPresent();
    }

    /**
     * Query whether the last operation was successful.
     *
     * @return true if the last Save/SaveAs operation completed successfully, false otherwise.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Query whether the last operation was canceled.
     *
     * @return true if the last Save/SaveAs operation was canceled from the file dialog or from another
     * query dialog, false otherwise.
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Check whether or not the external database has been modified. If so need to alert the user to accept external updates prior to
     * saving the database. This is necessary to avoid overwriting other users work when using a multiuser database file.
     *
     * @return true if the external database file has been modified and the user must choose to accept the changes and false if no modifications
     * were found or there is no requested protection for the database file.
     */
    private boolean checkExternalModification() {
        // Check for external modifications:
        if (panel.isUpdatedExternally()) {

            ButtonType save = new ButtonType(Localization.lang("Save"));
            ButtonType reviewChanges = new ButtonType(Localization.lang("Review changes"));

            Optional<ButtonType> buttonPressed = DefaultTaskExecutor.runInJavaFXThread(() -> frame.getDialogService().showCustomButtonDialogAndWait(AlertType.CONFIRMATION, Localization.lang("File updated externally"),
                                                                                                                                                    Localization.lang("File has been updated externally. " + "What do you want to do?"),
                                                                                                                                                    reviewChanges,
                                                                                                                                                    save,
                                                                                                                                                    ButtonType.CANCEL));

            if (buttonPressed.isPresent()) {
                if (buttonPressed.get() == ButtonType.CANCEL) {
                    canceled = true;
                    return true;
                }
                if (buttonPressed.get().equals(reviewChanges)) {
                    canceled = true;

                    JabRefExecutorService.INSTANCE.execute(() -> {

                        if (!FileBasedLock
                                .waitForFileLock(panel.getBibDatabaseContext().getDatabaseFile().get().toPath())) {
                            // TODO: GUI handling of the situation when the externally modified file keeps being locked.
                            LOGGER.error("File locked, this will be trouble.");
                        }

                        ChangeScanner scanner = new ChangeScanner(panel.frame(), panel,
                                panel.getBibDatabaseContext().getDatabaseFile().get(), panel.getTempFile());
                        JabRefExecutorService.INSTANCE.executeInterruptableTaskAndWait(scanner);
                        if (scanner.changesFound()) {
                            scanner.displayResult(resolved -> {
                                if (resolved) {
                                    panel.markExternalChangesAsResolved();
                                    DefaultTaskExecutor.runInJavaFXThread(() -> panel.getSidePaneManager().hide(SidePaneType.FILE_UPDATE_NOTIFICATION));
                                } else {
                                    canceled = true;
                                }
                            });
                        }
                    });

                    return true;
                } else { // User indicated to store anyway.
                    if (panel.getBibDatabaseContext().getMetaData().isProtected()) {

                        frame.getDialogService().showErrorDialogAndWait(Localization.lang("Protected library"),
                                Localization.lang("Library is protected. Cannot save until external changes have been reviewed."));

                        canceled = true;
                    } else {
                        panel.markExternalChangesAsResolved();
                        panel.getSidePaneManager().hide(SidePaneType.FILE_UPDATE_NOTIFICATION);
                    }
                }
            }
        }

        // Return false as either no external database file modifications have been found or overwrite is requested any way
        return false;
    }

    @Override
    public void action() throws Exception {
        init();
        BackgroundTask
                .wrap(this::doSave)
                .executeWith(Globals.TASK_EXECUTOR);
    }
}
