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

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

/**
 * Background task and utilities for autosave feature.
 */
public class AutoSaveManager {

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
        long interval = 60000 * Globals.prefs.getInt(JabRefPreferences.AUTO_SAVE_INTERVAL);
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

            List<BasePanel> panels = new ArrayList<>();
            for (int i = 0; i < frame.baseCount(); i++) {
                panels.add(frame.baseAt(i));
            }

            for (BasePanel panel : panels) {
                if (panel.isBaseChanged()) {
                    if (panel.getFile() != null) {
                        AutoSaveManager.autoSave(panel);
                    }
                }
            }
        }
    }


    /**
     * Get a File object pointing to the autosave file corresponding to the given file.
     * @param f The database file.
     * @return its corresponding autosave file.
     */
    public static File getAutoSaveFile(File f) {
        String n = f.getName();
        return new File(f.getParentFile(), ".$" + n + '$');
    }

    /**
     * Perform an autosave.
     * @param panel The BasePanel to autosave for.
     * @return true if successful, false otherwise.
     */
    private static boolean autoSave(BasePanel panel) {
        File backupFile = AutoSaveManager.getAutoSaveFile(panel.getFile());
        try {
            SaveSession ss = FileActions.saveDatabase(panel.database(), panel.metaData(),
                    backupFile, Globals.prefs,
                    false, false, panel.getEncoding(), true);
            ss.commit();
        } catch (SaveException e) {
            e.printStackTrace();
            return false;
        } catch (Throwable ex) {
            ex.printStackTrace();
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
        if (panel.getFile() == null) {
            return true;
        }
        File backupFile = AutoSaveManager.getAutoSaveFile(panel.getFile());
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
        List<BasePanel> panels = new ArrayList<>();
        for (int i = 0; i < frame.baseCount(); i++) {
            panels.add(frame.baseAt(i));
        }
        for (BasePanel panel : panels) {
            AutoSaveManager.deleteAutoSaveFile(panel);
        }
    }

    /**
     * Check if a newer autosave exists for the given file.
     * @param f The file to check.
     * @return true if an autosave is found, and if the autosave is newer
     *   than the given file.
     */
    public static boolean newerAutoSaveExists(File f) {
        File asFile = AutoSaveManager.getAutoSaveFile(f);
        return asFile.exists() && (asFile.lastModified() > f.lastModified());
    }
}
