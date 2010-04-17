package net.sf.jabref.export;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.*;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.collab.ChangeScanner;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Vector;

/**
 * Action for the "Save" and "Save as" operations called from BasePanel. This class is also used for
 * save operations when closing a database or quitting the applications.
 *
 * The operations run synchronously, but offload the save operation from the event thread using Spin.
 * Callers can query whether the operation was cancelled, or whether it was successful.
 */
public class SaveDatabaseAction extends AbstractWorker {
    private BasePanel panel;
    private JabRefFrame frame;
    private boolean success = false, cancelled = false, fileLockedError = false;

    public SaveDatabaseAction(BasePanel panel) {

        this.panel = panel;
        this.frame = panel.frame();
    }


    public void init() throws Throwable {
        success = false;
        cancelled = false;
        fileLockedError = false;
        if (panel.getFile() == null)
            saveAs();
        else {

            // Check for external modifications:
            if (panel.isUpdatedExternally() || Globals.fileUpdateMonitor.hasBeenModified(panel.getFileMonitorHandle())) {

                String[] opts = new String[]{Globals.lang("Review changes"), Globals.lang("Save"),
                        Globals.lang("Cancel")};
                int answer = JOptionPane.showOptionDialog(panel.frame(), Globals.lang("File has been updated externally. "
                        + "What do you want to do?"), Globals.lang("File updated externally"),
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, opts, opts[0]);
                /*  int choice = JOptionPane.showConfirmDialog(frame, Globals.lang("File has been updated externally. "
+"Are you sure you want to save?"), Globals.lang("File updated externally"),
               JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);*/

                if (answer == JOptionPane.CANCEL_OPTION) {
                    cancelled = true;
                    return;
                }
                else if (answer == JOptionPane.YES_OPTION) {
                    //try {

                    cancelled = true;

                    (new Thread(new Runnable() {
                        public void run() {

                            if (!Util.waitForFileLock(panel.getFile(), 10)) {
                                // TODO: GUI handling of the situation when the externally modified file keeps being locked.
                                System.err.println("File locked, this will be trouble.");
                            }

                            ChangeScanner scanner = new ChangeScanner(panel.frame(), panel);
                            scanner.changeScan(panel.getFile());
                            try {
                                scanner.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (scanner.changesFound()) {
                                scanner.displayResult(new ChangeScanner.DisplayResultCallback() {
                                    public void scanResultsResolved(boolean resolved) {
                                        if (!resolved) {
                                            cancelled = true;
                                        } else {
                                            panel.setUpdatedExternally(false);
                                            SwingUtilities.invokeLater(new Runnable() {
                                                public void run() {
                                                    panel.getSidePaneManager().hide("fileUpdate");
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        }
                    })).start();

                    return;
                }
                else { // User indicated to store anyway.
                    // See if the database has the protected flag set:
                    Vector<String> pd = panel.metaData().getData(Globals.PROTECTED_FLAG_META);
                    boolean databaseProtectionFlag = (pd != null) && Boolean.parseBoolean(pd.get(0));
                    if (databaseProtectionFlag) {
                        JOptionPane.showMessageDialog(frame, Globals.lang("Database is protected. Cannot save until external changes have been reviewed."),
                                Globals.lang("Protected database"), JOptionPane.ERROR_MESSAGE);
                        cancelled = true;
                    }
                    else {
                        panel.setUpdatedExternally(false);
                        panel.getSidePaneManager().hide("fileUpdate");
                    }
                }
            }

            panel.frame().output(Globals.lang("Saving database") + "...");
            panel.setSaving(true);
        }
    }

    public void update() {
        if (success) {
            // Reset title of tab
            frame.setTabTitle(panel, panel.getFile().getName(),
                    panel.getFile().getAbsolutePath());
            frame.output(Globals.lang("Saved database") + " '"
                    + panel.getFile().getPath() + "'.");
        } else if (!cancelled) {
            if (fileLockedError) {
                // TODO: user should have the option to override the lock file.
                frame.output(Globals.lang("Could not save, file locked by another JabRef instance."));
            } else
                frame.output(Globals.lang("Save failed"));
        }
    }

    public void run() {
        if (cancelled || (panel.getFile() == null)) {
            return;
        }

        try {

            // Make sure the current edit is stored:
            panel.storeCurrentEdit();

            // If the option is set, autogenerate keys for all entries that are
            // lacking keys, before saving:
            panel.autoGenerateKeysBeforeSaving();

            if (!Util.waitForFileLock(panel.getFile(), 10)) {
                success = false;
                fileLockedError = true;
            }
            else {
                // Now save the database:
                success = saveDatabase(panel.getFile(), false, panel.getEncoding());

                //Util.pr("Testing resolve string... BasePanel line 237");
                //Util.pr("Resolve aq: "+database.resolveString("aq"));
                //Util.pr("Resolve text: "+database.resolveForStrings("A text which refers to the string #aq# and #billball#, hurra."));

                try {
                    Globals.fileUpdateMonitor.updateTimeStamp(panel.getFileMonitorHandle());
                } catch (IllegalArgumentException ex) {
                    // This means the file has not yet been registered, which is the case
                    // when doing a "Save as". Maybe we should change the monitor so no
                    // exception is cast.
                }
            }
            panel.setSaving(false);
            if (success) {
                panel.undoManager.markUnchanged();

                if (!AutoSaveManager.deleteAutoSaveFile(panel)) {
                    //System.out.println("Deletion of autosave file failed");
                }/* else
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
                success =false;
                fileLockedError = true;
                return;
            }
            ex2.printStackTrace();
        }
    }

    private boolean saveDatabase(File file, boolean selectedOnly, String encoding) throws SaveException {
        SaveSession session;
        frame.block();
        try {
            if (!selectedOnly)
                session = FileActions.saveDatabase(panel.database(), panel.metaData(), file,
                        Globals.prefs, false, false, encoding, false);
            else
                session = FileActions.savePartOfDatabase(panel.database(), panel.metaData(), file,
                        Globals.prefs, panel.getSelectedEntries(), encoding);

        } catch (UnsupportedCharsetException ex2) {
            JOptionPane.showMessageDialog(frame, Globals.lang("Could not save file. "
                    + "Character encoding '%0' is not supported.", encoding),
                    Globals.lang("Save database"), JOptionPane.ERROR_MESSAGE);
            throw new SaveException("rt");
        } catch (SaveException ex) {
            if (ex == SaveException.FILE_LOCKED) {
                throw ex;
            }
            if (ex.specificEntry()) {
                // Error occured during processing of
                // be. Highlight it:
                int row = panel.mainTable.findEntry(ex.getEntry()),
                        topShow = Math.max(0, row - 3);
                panel.mainTable.setRowSelectionInterval(row, row);
                panel.mainTable.scrollTo(topShow);
                panel.showEntry(ex.getEntry());
            } else ex.printStackTrace();

            JOptionPane.showMessageDialog
                    (frame, Globals.lang("Could not save file")
                            + ".\n" + ex.getMessage(),
                            Globals.lang("Save database"),
                            JOptionPane.ERROR_MESSAGE);
            throw new SaveException("rt");

        } finally {
            frame.unblock();
        }

        boolean commit = true;
        if (!session.getWriter().couldEncodeAll()) {
            DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("left:pref, 4dlu, fill:pref", ""));
            JTextArea ta = new JTextArea(session.getWriter().getProblemCharacters());
            ta.setEditable(false);
            builder.append(Globals.lang("The chosen encoding '%0' could not encode the following characters: ",
                    session.getEncoding()));
            builder.append(ta);
            builder.append(Globals.lang("What do you want to do?"));
            String tryDiff = Globals.lang("Try different encoding");
            int answer = JOptionPane.showOptionDialog(frame, builder.getPanel(), Globals.lang("Save database"),
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new String[]{Globals.lang("Save"), tryDiff, Globals.lang("Cancel")}, tryDiff);

            if (answer == JOptionPane.NO_OPTION) {
                // The user wants to use another encoding.
                Object choice = JOptionPane.showInputDialog(frame, Globals.lang("Select encoding"), Globals.lang("Save database"),
                        JOptionPane.QUESTION_MESSAGE, null, Globals.ENCODINGS, encoding);
                if (choice != null) {
                    String newEncoding = (String) choice;
                    return saveDatabase(file, selectedOnly, newEncoding);
                } else
                    commit = false;
            } else if (answer == JOptionPane.CANCEL_OPTION)
                commit = false;


        }

        try {
            if (commit) {
                session.commit();
                panel.setEncoding(encoding); // Make sure to remember which encoding we used.
            } else
                session.cancel();
        } catch (SaveException e) {
            int ans = JOptionPane.showConfirmDialog(null, Globals.lang("Save failed during backup creation")+". "
                +Globals.lang("Save without backup?"), Globals.lang("Unable to create backup"),
                    JOptionPane.YES_NO_OPTION);
            if (ans == JOptionPane.YES_OPTION) {
                session.setUseBackup(false);
                session.commit();
                panel.setEncoding(encoding);
            }
            else commit = false;
        } catch (IOException e) {
            e.printStackTrace();
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
    }

    /**
     * Run the "Save as" operation. This method offloads the actual save operation to a background thread, but
     * still runs synchronously using Spin (the method returns only after completing the operation).
     */
    public void saveAs() throws Throwable {
        String chosenFile = null;
        File f = null;
        while (f == null) {
            chosenFile = FileDialogs.getNewFile(frame, new File(Globals.prefs.get("workingDirectory")), ".bib",
                    JFileChooser.SAVE_DIALOG, false, null);
            if (chosenFile == null) {
                cancelled = true;
                return; // cancelled
            }
            f = new File(chosenFile);
            // Check if the file already exists:
            if (f.exists() && (JOptionPane.showConfirmDialog
                    (frame, "'" + f.getName() + "' " + Globals.lang("exists. Overwrite file?"),
                            Globals.lang("Save database"), JOptionPane.OK_CANCEL_OPTION)
                    != JOptionPane.OK_OPTION)) {
                f = null;
            }
        }

        if (chosenFile != null) {
            File oldFile = panel.metaData().getFile();
            panel.metaData().setFile(f);
            Globals.prefs.put("workingDirectory", f.getParent());
            runCommand();
            // If the operation failed, revert the file field and return:
            if (!success) {
                panel.metaData().setFile(oldFile);
                return;
            }
            // Register so we get notifications about outside changes to the file.
            try {
                panel.setFileMonitorHandle(Globals.fileUpdateMonitor.addUpdateListener(panel, panel.getFile()));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            frame.getFileHistory().newFile(panel.metaData().getFile().getPath());
        }

    }

    /**
     * Query whether the last operation was successful.
     *
     * @returns true if the last Save/SaveAs operation completed successfully, false otherwise.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Query whether the last operation was cancelled.
     *
     * @returns true if the last Save/SaveAs operation was cancelled from the file dialog or from another 
     * query dialog, false otherwise.
     */
    public boolean isCancelled() {
        return cancelled;
    }
}
