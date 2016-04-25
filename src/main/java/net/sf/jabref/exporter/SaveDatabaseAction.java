/*  Copyright (C) 2003-2015 JabRef contributors.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.exporter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.collab.ChangeScanner;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.gui.worker.CallBack;
import net.sf.jabref.gui.worker.Worker;
import net.sf.jabref.logic.l10n.Encodings;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileBasedLock;

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

    private final BasePanel panel;
    private final JabRefFrame frame;
    private boolean success;
    private boolean canceled;
    private boolean fileLockedError;

    private static final Log LOGGER = LogFactory.getLog(SaveDatabaseAction.class);

    public SaveDatabaseAction(BasePanel panel) {
        this.panel = panel;
        this.frame = panel.frame();
    }

    @Override
    public void init() throws Throwable {
        success = false;
        canceled = false;
        fileLockedError = false;
        if (panel.getBibDatabaseContext().getDatabaseFile() == null) {
            saveAs();
        } else {

            // Check for external modifications: if true, save not performed so do not tell the user a save is underway but return instead.
            if (checkExternalModification()) {
                return;
            }

            panel.frame().output(Localization.lang("Saving database") + "...");
            panel.setSaving(true);
        }
    }

    @Override
    public void update() {
        if (success) {
            // Reset title of tab
            frame.setTabTitle(panel, panel.getTabTitle(), panel.getBibDatabaseContext().getDatabaseFile().getAbsolutePath());
            frame.output(Localization.lang("Saved database") + " '" + panel.getBibDatabaseContext().getDatabaseFile().getPath() + "'.");
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
        if (canceled || (panel.getBibDatabaseContext().getDatabaseFile() == null)) {
            return;
        }

        try {

            // Make sure the current edit is stored:
            panel.storeCurrentEdit();

            // If the option is set, autogenerate keys for all entries that are
            // lacking keys, before saving:
            panel.autoGenerateKeysBeforeSaving();

            if (FileBasedLock.waitForFileLock(panel.getBibDatabaseContext().getDatabaseFile(), 10)) {
                // Check for external modifications to alleviate multiuser concurrency issue when near
                // simultaneous saves occur to a shared database file: if true, do not perform the save
                // rather return instead.
                if (checkExternalModification()) {
                    return;
                }

                // Save the database:
                success = saveDatabase(panel.getBibDatabaseContext().getDatabaseFile(), false, panel.getEncoding());

                Globals.fileUpdateMonitor.updateTimeStamp(panel.getFileMonitorHandle());
            } else {
                // No file lock
                success = false;
                fileLockedError = true;
            }
            panel.setSaving(false);
            if (success) {
                panel.undoManager.markUnchanged();

                if (!AutoSaveManager.deleteAutoSaveFile(panel)) {
                    //System.out.println("Deletion of autosave file failed");
                } /* else
                     System.out.println("Deleted autosave file (if it existed)");*/
                // (Only) after a successful save the following
                // statement marks that the base is unchanged
                // since last save:
                panel.setNonUndoableChange(false);
                panel.setBaseChanged(false);
                panel.setUpdatedExternally(false);
            }
        } catch (SaveException ex2) {
            if (ex2 == SaveException.FILE_LOCKED) {
                success = false;
                fileLockedError = true;
                return;
            }
            LOGGER.error("Problem saving file", ex2);
        }
    }

    private boolean saveDatabase(File file, boolean selectedOnly, Charset encoding) throws SaveException {
        SaveSession session;
        frame.block();
        try {
            SavePreferences prefs = SavePreferences.loadForSaveFromPreferences(Globals.prefs).withEncoding(encoding);
            BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
            if (selectedOnly) {
                session = databaseWriter.savePartOfDatabase(panel.getBibDatabaseContext(), prefs,
                        panel.getSelectedEntries());

            } else {
                session = databaseWriter.saveDatabase(panel.getBibDatabaseContext(), prefs);

            }
            panel.registerUndoableChanges(session);

        } catch (UnsupportedCharsetException ex2) {
            JOptionPane.showMessageDialog(frame, Localization.lang("Could not save file.") +
                            Localization.lang("Character encoding '%0' is not supported.", encoding.displayName()),
                    Localization.lang("Save database"), JOptionPane.ERROR_MESSAGE);
            throw new SaveException("rt");
        } catch (SaveException ex) {
            if (ex == SaveException.FILE_LOCKED) {
                throw ex;
            }
            if (ex.specificEntry()) {
                // Error occured during processing of
                // be. Highlight it:
                int row = panel.mainTable.findEntry(ex.getEntry());
                int topShow = Math.max(0, row - 3);
                panel.mainTable.setRowSelectionInterval(row, row);
                panel.mainTable.scrollTo(topShow);
                panel.showEntry(ex.getEntry());
            } else {
                LOGGER.error("Problem saving file", ex);
            }

            JOptionPane.showMessageDialog
                    (frame, Localization.lang("Could not save file.")
                            + ".\n" + ex.getMessage(),
                            Localization.lang("Save database"),
                            JOptionPane.ERROR_MESSAGE);
            throw new SaveException("rt");

        } finally {
            frame.unblock();
        }

        boolean commit = true;
        if (!session.getWriter().couldEncodeAll()) {
            FormBuilder builder = FormBuilder.create().layout(new FormLayout("left:pref, 4dlu, fill:pref", "pref, 4dlu, pref"));
            JTextArea ta = new JTextArea(session.getWriter().getProblemCharacters());
            ta.setEditable(false);
            builder.add(Localization.lang("The chosen encoding '%0' could not encode the following characters:",
                    session.getEncoding().displayName())).xy(1, 1);
            builder.add(ta).xy(3, 1);
            builder.add(Localization.lang("What do you want to do?")).xy(1, 3);
            String tryDiff = Localization.lang("Try different encoding");
            int answer = JOptionPane.showOptionDialog(frame, builder.getPanel(), Localization.lang("Save database"),
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new String[] {Localization.lang("Save"), tryDiff,
                            Localization.lang("Cancel")}, tryDiff);

            if (answer == JOptionPane.NO_OPTION) {
                // The user wants to use another encoding.
                Object choice = JOptionPane.showInputDialog(frame, Localization.lang("Select encoding"),
                        Localization.lang("Save database"), JOptionPane.QUESTION_MESSAGE, null,
                        Encodings.ENCODINGS_DISPLAYNAMES, encoding);
                if (choice == null) {
                    commit = false;
                } else {
                    Charset newEncoding = Charset.forName((String) choice);
                    return saveDatabase(file, selectedOnly, newEncoding);
                }
            } else if (answer == JOptionPane.CANCEL_OPTION) {
                commit = false;
            }

        }

        try {
            if (commit) {
                session.commit(file);
                panel.setEncoding(encoding); // Make sure to remember which encoding we used.
            } else {
                session.cancel();
            }
        } catch (SaveException e) {
            int ans = JOptionPane.showConfirmDialog(null, Localization.lang("Save failed during backup creation") + ". "
                    + Localization.lang("Save without backup?"),
                    Localization.lang("Unable to create backup"),
                    JOptionPane.YES_NO_OPTION);
            if (ans == JOptionPane.YES_OPTION) {
                session.setUseBackup(false);
                session.commit(file);
                panel.setEncoding(encoding);
            } else {
                commit = false;
            }
        }

        return commit;
    }

    /**
     * Run the "Save" operation. This method offloads the actual save operation to a background thread, but
     * still runs synchronously using Spin (the method returns only after completing the operation).
     */
    public void runCommand() throws Throwable {
        // This part uses Spin's features:
        Worker wrk = getWorker();
        // The Worker returned by getWorker() has been wrapped
        // by Spin.off(), which makes its methods be run in
        // a different thread from the EDT.
        CallBack clb = getCallBack();

        init(); // This method runs in this same thread, the EDT.
        // Useful for initial GUI actions, like printing a message.

        // The CallBack returned by getCallBack() has been wrapped
        // by Spin.over(), which makes its methods be run on
        // the EDT.
        wrk.run(); // Runs the potentially time-consuming action
        // without freezing the GUI. The magic is that THIS line
        // of execution will not continue until run() is finished.
        clb.update(); // Runs the update() method on the EDT.

    }

    public void save() throws Throwable {
        runCommand();
        frame.updateEnabledState();
    }

    /**
     * Run the "Save as" operation. This method offloads the actual save operation to a background thread, but
     * still runs synchronously using Spin (the method returns only after completing the operation).
     */
    public void saveAs() throws Throwable {
        String chosenFile;
        File f = null;
        while (f == null) {
            chosenFile = FileDialogs.getNewFile(frame, new File(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)), ".bib",
                    JFileChooser.SAVE_DIALOG, false, null);
            if (chosenFile == null) {
                canceled = true;
                return; // canceled
            }
            f = new File(chosenFile);
            // Check if the file already exists:
            if (f.exists() && (JOptionPane.showConfirmDialog(frame,
                    Localization.lang("'%0' exists. Overwrite file?", f.getName()),
                    Localization.lang("Save database"), JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)) {
                f = null;
            }
        }

        if (f != null) {
            File oldFile = panel.getBibDatabaseContext().getDatabaseFile();
            panel.getBibDatabaseContext().setDatabaseFile(f);
            Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, f.getParent());
            runCommand();
            // If the operation failed, revert the file field and return:
            if (!success) {
                panel.getBibDatabaseContext().setDatabaseFile(oldFile);
                return;
            }
            // Register so we get notifications about outside changes to the file.
            try {
                panel.setFileMonitorHandle(Globals.fileUpdateMonitor.addUpdateListener(panel,
                        panel.getBibDatabaseContext().getDatabaseFile()));
            } catch (IOException ex) {
                LOGGER.error("Problem registering file change notifications", ex);
            }
            frame.getFileHistory().newFile(panel.getBibDatabaseContext().getDatabaseFile().getPath());
        }
        frame.updateEnabledState();
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
        if (panel.isUpdatedExternally() || Globals.fileUpdateMonitor.hasBeenModified(panel.getFileMonitorHandle())) {
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

                JabRefExecutorService.INSTANCE.execute((Runnable) () -> {

                    if (!FileBasedLock.waitForFileLock(panel.getBibDatabaseContext().getDatabaseFile(), 10)) {
                        // TODO: GUI handling of the situation when the externally modified file keeps being locked.
                        LOGGER.error("File locked, this will be trouble.");
                    }

                    ChangeScanner scanner = new ChangeScanner(panel.frame(), panel,
                            panel.getBibDatabaseContext().getDatabaseFile());
                    JabRefExecutorService.INSTANCE.executeWithLowPriorityInOwnThreadAndWait(scanner);
                    if (scanner.changesFound()) {
                        scanner.displayResult((ChangeScanner.DisplayResultCallback) resolved -> {
                            if (resolved) {
                                panel.setUpdatedExternally(false);
                                SwingUtilities
                                        .invokeLater((Runnable) () -> panel.getSidePaneManager().hide("fileUpdate"));
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
                    panel.getSidePaneManager().hide("fileUpdate");
                }
            }
        }

        // Return false as either no external database file modifications have been found or overwrite is requested any way
        return false;
    }
}
