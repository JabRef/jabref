package org.jabref.gui.exporter;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.Optional;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.collab.ChangeScanner;
import org.jabref.gui.collab.FileUpdatePanel;
import org.jabref.gui.dialogs.AutosaveUIManager;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.worker.AbstractWorker;
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
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.io.FileBasedLock;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.ChangePropagation;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for the "Save" and "Save as" operations called from BasePanel. This class is also used for
 * save operations when closing a database or quitting the applications.
 *
 * The operations run synchronously, but offload the save operation from the event thread using Spin.
 * Callers can query whether the operation was canceled, or whether it was successful.
 */
public class SaveDatabaseAction extends AbstractWorker {

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

    @Override
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

    @Override
    public void update() {
        if (success) {
            // Reset title of tab
            frame.setTabTitle(panel, panel.getTabTitle(),
                    panel.getBibDatabaseContext().getDatabaseFile().get().getAbsolutePath());
            frame.output(Localization.lang("Saved library") + " '"
                    + panel.getBibDatabaseContext().getDatabaseFile().get().getPath() + "'.");
            frame.setWindowTitle();
            frame.updateAllTabTitles();
        } else if (!canceled) {
            if (fileLockedError) {
                // TODO: user should have the option to override the lock file.
                frame.output(Localization.lang("Could not save, file locked by another JabRef instance."));
            } else {
                frame.output(Localization.lang("Save failed"));
            }
        }
    }

    @Override
    public void run() {
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
                        panel.getBibDatabaseContext().getMetaData().getEncoding()
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
    }

    private boolean saveDatabase(File file, boolean selectedOnly, Charset encoding) throws SaveException {
        SaveSession session;

        // block user input
        frame.block();

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
            JOptionPane.showMessageDialog(frame,
                    Localization.lang("Could not save file.")
                            + Localization.lang("Character encoding '%0' is not supported.", encoding.displayName()),
                    Localization.lang("Save library"), JOptionPane.ERROR_MESSAGE);
            // FIXME: rethrow anti-pattern
            throw new SaveException("rt");
        } catch (SaveException ex) {
            if (ex == SaveException.FILE_LOCKED) {
                throw ex;
            }
            if (ex.specificEntry()) {
                BibEntry entry = ex.getEntry();
                // Error occured during processing of an entry. Highlight it!
                panel.highlightEntry(entry);
            } else {
                LOGGER.error("A problem occured when trying to save the file", ex);
            }

            JOptionPane.showMessageDialog(frame, Localization.lang("Could not save file.") + ".\n" + ex.getMessage(),
                    Localization.lang("Save library"), JOptionPane.ERROR_MESSAGE);
            // FIXME: rethrow anti-pattern
            throw new SaveException("rt");
        } finally {
            // re-enable user input
            frame.unblock();
        }

        // handle encoding problems
        boolean success = true;
        if (!session.getWriter().couldEncodeAll()) {
            FormBuilder builder = FormBuilder.create()
                    .layout(new FormLayout("left:pref, 4dlu, fill:pref", "pref, 4dlu, pref"));
            JTextArea ta = new JTextArea(session.getWriter().getProblemCharacters());
            ta.setEditable(false);
            builder.add(Localization.lang("The chosen encoding '%0' could not encode the following characters:",
                    session.getEncoding().displayName())).xy(1, 1);
            builder.add(ta).xy(3, 1);
            builder.add(Localization.lang("What do you want to do?")).xy(1, 3);
            String tryDiff = Localization.lang("Try different encoding");
            int answer = JOptionPane.showOptionDialog(frame, builder.getPanel(), Localization.lang("Save library"),
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new String[] {Localization.lang("Save"), tryDiff, Localization.lang("Cancel")}, tryDiff);

            if (answer == JOptionPane.NO_OPTION) {
                // The user wants to use another encoding.
                Object choice = JOptionPane.showInputDialog(frame, Localization.lang("Select encoding"),
                        Localization.lang("Save library"), JOptionPane.QUESTION_MESSAGE, null,
                        Encodings.ENCODINGS_DISPLAYNAMES, encoding);
                if (choice == null) {
                    success = false;
                } else {
                    Charset newEncoding = Charset.forName((String) choice);
                    return saveDatabase(file, selectedOnly, newEncoding);
                }
            } else if (answer == JOptionPane.CANCEL_OPTION) {
                success = false;
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
            int ans = JOptionPane.showConfirmDialog(null,
                    Localization.lang("Save failed during backup creation") + ". "
                            + Localization.lang("Save without backup?"),
                    Localization.lang("Unable to create backup"), JOptionPane.YES_NO_OPTION);
            if (ans == JOptionPane.YES_OPTION) {
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
     * Run the "Save" operation. This method offloads the actual save operation to a background thread, but
     * still runs synchronously using Spin (the method returns only after completing the operation).
     */
    public void runCommand() throws Exception {
        BasePanel.runWorker(this);
    }

    public void save() throws Exception {
        runCommand();
        frame.updateEnabledState();
    }

    public void saveAs() throws Exception {
        // configure file dialog

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(FileType.BIBTEX_DB)
                .withDefaultExtension(FileType.BIBTEX_DB)
                .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();
        DialogService ds = new FXDialogService();

        Optional<Path> path = DefaultTaskExecutor
                .runInJavaFXThread(() -> ds.showFileSaveDialog(fileDialogConfiguration));
        if (path.isPresent()) {
            saveAs(path.get().toFile());
        } else {
            canceled = true;
            return;
        }
    }

    /**
     * Run the "Save as" operation. This method offloads the actual save operation to a background thread, but
     * still runs synchronously using Spin (the method returns only after completing the operation).
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
        frame.updateEnabledState();
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
            String[] opts = new String[] {Localization.lang("Review changes"), Localization.lang("Save"),
                    Localization.lang("Cancel")};
            int answer = JOptionPane.showOptionDialog(panel.frame(),
                    Localization.lang("File has been updated externally. " + "What do you want to do?"),
                    Localization.lang("File updated externally"), JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, opts, opts[0]);

            if (answer == JOptionPane.CANCEL_OPTION) {
                canceled = true;
                return true;
            } else if (answer == JOptionPane.YES_OPTION) {
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
                                SwingUtilities
                                        .invokeLater(() -> panel.getSidePaneManager().hide(FileUpdatePanel.class));
                            } else {
                                canceled = true;
                            }
                        });
                    }
                });

                return true;
            } else { // User indicated to store anyway.
                if (panel.getBibDatabaseContext().getMetaData().isProtected()) {
                    JOptionPane.showMessageDialog(frame,
                            Localization
                                    .lang("Library is protected. Cannot save until external changes have been reviewed."),
                            Localization.lang("Protected library"), JOptionPane.ERROR_MESSAGE);
                    canceled = true;
                } else {
                    panel.markExternalChangesAsResolved();
                    panel.getSidePaneManager().hide(FileUpdatePanel.class);
                }
            }
        }

        // Return false as either no external database file modifications have been found or overwrite is requested any way
        return false;
    }
}
