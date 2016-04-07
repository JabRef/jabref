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
package net.sf.jabref.importer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.ParserResultWarningDialog;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.exporter.AutoSaveManager;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Runnable task that prompts the user for what to do about files loaded at startup,
 * where an autosave file was found. The task should be run on the EDT after startup.
 */
public class AutosaveStartupPrompter implements Runnable {

    private final JabRefFrame frame;
    private final List<File> files;


    public AutosaveStartupPrompter(JabRefFrame frame, List<File> files) {

        this.frame = frame;
        this.files = files;
    }

    @Override
    public void run() {
        boolean first = frame.getBasePanelCount() == 0;
        List<ParserResult> loaded = new ArrayList<>();
        Map<ParserResult, Integer> location = new HashMap<>();
        for (File file : files) {
            File fileToLoad = file;
            boolean tryingAutosave;
            if (Globals.prefs.getBoolean(JabRefPreferences.PROMPT_BEFORE_USING_AUTOSAVE)) {
                int answer = JOptionPane.showConfirmDialog(null, "<html>" +
                        Localization.lang("An autosave file was found for this database. This could indicate "
                                + "that JabRef didn't shut down cleanly last time the file was used.") + "<br>"
                        + Localization.lang("Do you want to recover the database from the autosave file?") + "</html>",
                        Localization.lang("Autosave of file '%0'", file.getName()), JOptionPane.YES_NO_OPTION);
                tryingAutosave = answer == JOptionPane.YES_OPTION;
            } else {
                tryingAutosave = true;
            }

            if (tryingAutosave) {
                fileToLoad = AutoSaveManager.getAutoSaveFile(file);
            }
            boolean done = false;
            ParserResult pr = null;
            while (!done) {
                pr = JabRef.openBibFile(fileToLoad.getPath(), true);
                if ((pr != null) && !pr.isInvalid()) {
                    loaded.add(pr);
                    BibDatabaseContext databaseContext = pr.getDatabaseContext();
                    databaseContext.setDatabaseFile(file);
                    BasePanel panel = frame.addTab(databaseContext, pr.getEncoding(), first);
                    location.put(pr, frame.getBasePanelCount() - 1);
                    if (tryingAutosave) {
                        panel.markNonUndoableBaseChanged();
                    }

                    first = false;
                    done = true;
                } else {
                    if (tryingAutosave) {
                        JOptionPane.showMessageDialog(frame,
                                Localization.lang("Error opening autosave of '%0'. Trying to load '%0' instead.", file.getName()),
                                Localization.lang("Error opening file"), JOptionPane.ERROR_MESSAGE);
                        tryingAutosave = false;
                        fileToLoad = file;
                    } else {
                        String message;
                        if (pr == null) {
                            message = Localization.lang("Error opening file '%0'.", file.getName());
                        } else {
                            message = "<html>" + pr.getErrorMessage() + "<p>" +
                                    Localization.lang("Error opening file '%0'.", file.getName()) + "</html>";
                        }
                        JOptionPane.showMessageDialog(frame,
                                message, Localization.lang("Error opening file"), JOptionPane.ERROR_MESSAGE);
                        done = true;
                    }

                }
            }

            if ((pr != null) && !pr.isInvalid()
                    && Globals.prefs.getBoolean(JabRefPreferences.DISPLAY_KEY_WARNING_DIALOG_AT_STARTUP)) {
                ParserResultWarningDialog.showParserResultWarningDialog(pr, frame);
            }
        }
    }
}
