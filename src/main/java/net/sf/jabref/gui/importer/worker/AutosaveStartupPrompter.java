package net.sf.jabref.gui.importer.worker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.importer.ParserResultWarningDialog;
import net.sf.jabref.logic.importer.OpenDatabase;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.AutoSaveUtil;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.preferences.JabRefPreferences;

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
                                + "that JabRef did not shut down cleanly last time the file was used.") + "<br>"
                        + Localization.lang("Do you want to recover the database from the autosave file?") + "</html>",
                        Localization.lang("Autosave of file '%0'", file.getName()), JOptionPane.YES_NO_OPTION);
                tryingAutosave = answer == JOptionPane.YES_OPTION;
            } else {
                tryingAutosave = true;
            }

            if (tryingAutosave) {
                fileToLoad = AutoSaveUtil.getAutoSaveFile(file);
            }
            boolean done = false;
            ParserResult pr;
            do {
                pr = OpenDatabase.loadDatabaseOrAutoSave(fileToLoad.getPath(), true,
                        Globals.prefs.getImportFormatPreferences());
                if (pr.isInvalid()) {
                    loaded.add(pr);
                    BibDatabaseContext databaseContext = pr.getDatabaseContext();
                    databaseContext.setDatabaseFile(file);
                    BasePanel panel = frame.addTab(databaseContext, first);
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
                        String message = "<html>" + pr.getErrorMessage() + "<p>"
                                + Localization.lang("Error opening file '%0'.", file.getName()) + "</html>";
                        JOptionPane.showMessageDialog(frame,
                                message, Localization.lang("Error opening file"), JOptionPane.ERROR_MESSAGE);
                        done = true;
                    }

                }
            } while (!done);

            if (!pr.isInvalid()) {
                ParserResultWarningDialog.showParserResultWarningDialog(pr, frame);
            }
        }
    }
}
