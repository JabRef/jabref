package net.sf.jabref.gui.exporter;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.exporter.BibDatabaseWriter;
import net.sf.jabref.logic.exporter.BibtexDatabaseWriter;
import net.sf.jabref.logic.exporter.FileSaveSession;
import net.sf.jabref.logic.exporter.SaveException;
import net.sf.jabref.logic.exporter.SavePreferences;
import net.sf.jabref.logic.exporter.SaveSession;
import net.sf.jabref.logic.util.io.AutoSaveUtil;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Background task and utilities for autosave feature.
 */
public class AutoSaveManager {

    private static final Log LOGGER = LogFactory.getLog(AutoSaveManager.class);

    private final JabRefFrame frame;
    private Timer t;

    public AutoSaveManager(JabRefFrame frame) {
        this.frame = frame;
    }

    public void startAutoSaveTimer() {
        if(t != null) {
            // shut down any previously set timer to not leak any timers
            t.cancel();
        }

        TimerTask task = new AutoSaveTask();
        t = new Timer();
        long interval = (long) 60000 * Globals.prefs.getInt(JabRefPreferences.AUTO_SAVE_INTERVAL);
        t.scheduleAtFixedRate(task, interval, interval);
    }

    public void stopAutoSaveTimer() {
        t.cancel();
    }


    private class AutoSaveTask extends TimerTask {

        @Override
        public void run() {
            // Since this method is running in the background, we must be prepared that
            // there could be changes done by the user while this method is running.

            for (BasePanel panel : frame.getBasePanelList()) {
                if (panel.isModified() && (panel.getBibDatabaseContext().getDatabaseFile() != null)) {
                        AutoSaveManager.autoSave(panel);
                }
            }
        }
    }


    /**
     * Perform an autosave.
     * @param panel The BasePanel to autosave for.
     * @return true if successful, false otherwise.
     */
    private static boolean autoSave(BasePanel panel) {
        File databaseFile = panel.getBibDatabaseContext().getDatabaseFile();
        File backupFile = AutoSaveUtil.getAutoSaveFile(databaseFile);
        try {
            SavePreferences prefs = SavePreferences.loadForSaveFromPreferences(Globals.prefs)
                    .withMakeBackup(false)
                    .withEncoding(panel.getBibDatabaseContext().getMetaData().getEncoding());

            BibDatabaseWriter databaseWriter = new BibtexDatabaseWriter(FileSaveSession::new);
            SaveSession ss = databaseWriter.saveDatabase(panel.getBibDatabaseContext(), prefs);
            ss.commit(backupFile.toPath());
        } catch (SaveException e) {
            LOGGER.error("Problem with automatic save", e);
            return false;
        }
        return true;
    }

    /**
     * Delete this BasePanel's autosave if it exists.
     * @param panel The BasePanel in question.
     * @return true if there was no autosave or if the autosave was successfully deleted, false otherwise.
     */
    public static boolean deleteAutoSaveFile(BasePanel panel) {
        if (panel.getBibDatabaseContext().getDatabaseFile() == null) {
            return true;
        }
        File backupFile = AutoSaveUtil.getAutoSaveFile(panel.getBibDatabaseContext().getDatabaseFile());
        if (backupFile.exists()) {
            return backupFile.delete();
        } else {
            return true;
        }
    }

    /**
     * Clean up by deleting the autosave files corresponding to all open files,
     * if they exist.
     */
    public void clearAutoSaves() {
        for (BasePanel panel : frame.getBasePanelList()) {
            AutoSaveManager.deleteAutoSaveFile(panel);
        }
    }
}
