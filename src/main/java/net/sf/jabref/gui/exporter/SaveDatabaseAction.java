package net.sf.jabref.gui.exporter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.Optional;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.collab.ChangeScanner;
import net.sf.jabref.collab.FileUpdatePanel;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.FileDialog;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.autosaveandbackup.AutosaveUIManager;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.gui.worker.CallBack;
import net.sf.jabref.gui.worker.Worker;
import net.sf.jabref.logic.autosaveandbackup.AutosaveManager;
import net.sf.jabref.logic.autosaveandbackup.BackupManager;
import net.sf.jabref.logic.exporter.BibtexDatabaseWriter;
import net.sf.jabref.logic.exporter.FileSaveSession;
import net.sf.jabref.logic.exporter.SaveException;
import net.sf.jabref.logic.exporter.SavePreferences;
import net.sf.jabref.logic.exporter.SaveSession;
import net.sf.jabref.logic.l10n.Encodings;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.logic.util.io.FileBasedLock;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.database.DatabaseLocation;
import net.sf.jabref.model.database.event.ChangePropagation;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;
import net.sf.jabref.shared.DBMSConnectionProperties;
import net.sf.jabref.shared.prefs.SharedDatabasePreferences;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Action for the "Save" and "Save as" operations called from BasePanel. This class is also used for
 * save operations when closing a database or quitting the applications.
 *
 * The operations run synchronously, but offload the save operation from the event thread using Spin.
 * Callers can query whether the operation was canceled, or whether it was successful.
 */
public class SaveDatabaseAction extends AbstractWorker {
    private static final Log LOGGER = LogFactory.getLog(SaveDatabaseAction.class);

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

            panel.frame().output(Localization.lang("Saving database") + "...");
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
            frame.output(Localization.lang("Saved database") + " '"
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
            // Make sure the current edit is stored
            panel.storeCurrentEdit();

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
                        panel.getBibDatabaseContext().getMetaData().getEncoding().orElse(Globals.prefs.getDefaultEncoding()));

                Globals.getFileUpdateMonitor().updateTimeStamp(panel.getFileMonitorHandle());
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
                panel.setUpdatedExternally(false);
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
            SavePreferences prefs = SavePreferences.loadForSaveFromPreferences(Globals.prefs).withEncoding(encoding);
            BibtexDatabaseWriter<SaveSession> databaseWriter = new BibtexDatabaseWriter<>(FileSaveSession::new);

            if (selectedOnly) {
                session = databaseWriter.savePartOfDatabase(panel.getBibDatabaseContext(), panel.getSelectedEntries(), prefs);
            } else {
                session = databaseWriter.saveDatabase(panel.getBibDatabaseContext(), prefs);
            }

            panel.registerUndoableChanges(session);

        } catch (UnsupportedCharsetException ex) {
            JOptionPane.showMessageDialog(frame,
                    Localization.lang("Could not save file.")
                            + Localization.lang("Character encoding '%0' is not supported.", encoding.displayName()),
                    Localization.lang("Save database"), JOptionPane.ERROR_MESSAGE);
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
                    Localization.lang("Save database"), JOptionPane.ERROR_MESSAGE);
            // FIXME: rethrow anti-pattern
            throw new SaveException("rt");
        } finally {
            // re-enable user input
            frame.unblock();
        }

        // handle encoding problems
        boolean success = true;
        if (!session.getWriter().couldEncodeAll()) {
            FormBuilder builder = FormBuilder.create().layout(new FormLayout("left:pref, 4dlu, fill:pref", "pref, 4dlu, pref"));
            JTextArea ta = new JTextArea(session.getWriter().getProblemCharacters());
            ta.setEditable(false);
            builder.add(Localization.lang("The chosen encoding '%0' could not encode the following characters:", session.getEncoding().displayName())).xy(1, 1);
            builder.add(ta).xy(3, 1);
            builder.add(Localization.lang("What do you want to do?")).xy(1, 3);
            String tryDiff = Localization.lang("Try different encoding");
            int answer = JOptionPane.showOptionDialog(frame, builder.getPanel(), Localization.lang("Save database"),
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new String[] {Localization.lang("Save"), tryDiff, Localization.lang("Cancel")}, tryDiff);

            if (answer == JOptionPane.NO_OPTION) {
                // The user wants to use another encoding.
                Object choice = JOptionPane.showInputDialog(frame, Localization.lang("Select encoding"),
                        Localization.lang("Save database"), JOptionPane.QUESTION_MESSAGE, null,
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
        // This part uses Spin's features:
        Worker worker = getWorker();
        // The Worker returned by getWorker() has been wrapped
        // by Spin.off(), which makes its methods be run in
        // a different thread from the EDT.
        CallBack callback = getCallBack();

        init(); // This method runs in this same thread, the EDT.
        // Useful for initial GUI actions, like printing a message.

        // The CallBack returned by getCallBack() has been wrapped
        // by Spin.over(), which makes its methods be run on
        // the EDT.
        worker.run(); // Runs the potentially time-consuming action
        // without freezing the GUI. The magic is that THIS line
        // of execution will not continue until run() is finished.
        callback.update(); // Runs the update() method on the EDT.
    }

    public void save() throws Exception {
        runCommand();
        frame.updateEnabledState();
    }

    public void saveAs() throws Exception {
        // configure file dialog
        FileDialog dialog = new FileDialog(frame);
        dialog.withExtension(FileExtensions.BIBTEX_DB);
        dialog.setDefaultExtension(FileExtensions.BIBTEX_DB);

        Optional<Path> path = dialog.saveNewFile();
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
            DBMSConnectionProperties properties = context.getDBMSSynchronizer().getDBProcessor().getDBMSConnectionProperties();
            new SharedDatabasePreferences(context.getDatabase().generateSharedDatabaseID()).putAllDBMSConnectionProperties(properties);
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
        // Register so we get notifications about outside changes to the file.

        try {
            panel.setFileMonitorHandle(Globals.getFileUpdateMonitor().addUpdateListener(panel,
                    context.getDatabaseFile().orElse(null)));
        } catch (IOException ex) {
            LOGGER.error("Problem registering file change notifications", ex);
        }

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
        return (context.getLocation() == DatabaseLocation.SHARED ||
                ((context.getLocation() == DatabaseLocation.LOCAL) && Globals.prefs.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE))) &&
                context.getDatabaseFile().isPresent();
    }

    private boolean readyForBackup(BibDatabaseContext context) {
        return context.getLocation() == DatabaseLocation.LOCAL && context.getDatabaseFile().isPresent();
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
        if (panel.isUpdatedExternally()
                || Globals.getFileUpdateMonitor().hasBeenModified(panel.getFileMonitorHandle())) {
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
                            panel.getBibDatabaseContext().getDatabaseFile().get());
                    JabRefExecutorService.INSTANCE.executeWithLowPriorityInOwnThreadAndWait(scanner);
                    if (scanner.changesFound()) {
                        scanner.displayResult(resolved -> {
                            if (resolved) {
                                panel.setUpdatedExternally(false);
                                SwingUtilities.invokeLater(() -> panel.getSidePaneManager().hide(FileUpdatePanel.class));
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
                                    .lang("Database is protected. Cannot save until external changes have been reviewed."),
                            Localization.lang("Protected database"), JOptionPane.ERROR_MESSAGE);
                    canceled = true;
                } else {
                    panel.setUpdatedExternally(false);
                    panel.getSidePaneManager().hide(FileUpdatePanel.class);
                }
            }
        }

        // Return false as either no external database file modifications have been found or overwrite is requested any way
        return false;
    }
}
