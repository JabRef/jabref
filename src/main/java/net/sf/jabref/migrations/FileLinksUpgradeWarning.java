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
package net.sf.jabref.migrations;

import java.util.Arrays;
import java.util.List;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.actions.BrowseAction;
import net.sf.jabref.gui.entryeditor.EntryEditorTabList;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.PostOpenAction;
import net.sf.jabref.gui.undo.NamedCompound;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.cleanup.UpgradePdfPsToFileCleanup;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

/**
 * This class defines the warning that can be offered when opening a pre-2.3
 * JabRef file into a later version. This warning mentions the new external file
 * link system in this version of JabRef, and offers to:
 *
 * * upgrade old-style PDF/PS links into the "file" field
 * * modify General fields to show "file" instead of "pdf" / "ps"
 * * modify table column settings to show "file" instead of "pdf" / "ps"
 */
public class FileLinksUpgradeWarning implements PostOpenAction {

    private static final String[] FIELDS_TO_LOOK_FOR = new String[] {"pdf", "ps", "evastar_pdf"};

    private boolean offerChangeSettings;

    private boolean offerChangeDatabase;

    private boolean offerSetFileDir;


    /**
     * This method should be performed if the major/minor versions recorded in the ParserResult
     * are less than or equal to 2.2.
     * @param pr
     * @return true if the file was written by a jabref version <=2.2
     */
    @Override
    public boolean isActionNecessary(ParserResult pr) {
        // Find out which actions should be offered:
        // Only offer to change Preferences if file column is not already visible:
        offerChangeSettings = !Globals.prefs.getBoolean(JabRefPreferences.FILE_COLUMN) || !showsFileInGenFields();
        // Only offer to upgrade links if the pdf/ps fields are used:
        offerChangeDatabase = linksFound(pr.getDatabase(), FileLinksUpgradeWarning.FIELDS_TO_LOOK_FOR);
        // If the "file" directory is not set, offer to migrate pdf/ps dir:
        offerSetFileDir = !Globals.prefs.hasKey(Globals.FILE_FIELD + Globals.DIR_SUFFIX)
                && (Globals.prefs.hasKey("pdfDirectory") || Globals.prefs.hasKey("psDirectory"));

        // First check if this warning is disabled:
        return Globals.prefs.getBoolean(JabRefPreferences.SHOW_FILE_LINKS_UPGRADE_WARNING) && isThereSomethingToBeDone();
    }

    /**
     * This method presents a dialog box explaining and offering to make the
     * changes. If the user confirms, the changes are performed.
     * @param panel
     * @param parserResult
     */
    @Override
    public void performAction(BasePanel panel, ParserResult parserResult) {


        if (!isThereSomethingToBeDone())         {
            return; // Nothing to do, just return.
        }

        JCheckBox changeSettings = new JCheckBox(Localization.lang("Change table column and General fields settings to use the new feature"),
                offerChangeSettings);
        JCheckBox changeDatabase = new JCheckBox(Localization.lang("Upgrade old external file links to use the new feature"),
                offerChangeDatabase);
        JCheckBox setFileDir = new JCheckBox(Localization.lang("Set main external file directory") + ":", offerSetFileDir);
        JTextField fileDir = new JTextField(30);
        JCheckBox doNotShowDialog = new JCheckBox(Localization.lang("Do not show these options in the future"),
                false);

        JPanel message = new JPanel();
        FormBuilder formBuilder = FormBuilder.create().layout(new FormLayout("left:pref", "p"));
        // Keep the formatting of these lines. Otherwise, strings have to be translated again.
        // See updated JabRef_en.properties modifications by python syncLang.py -s -u
        int row = 1;
        formBuilder.add(new JLabel("<html>" + Localization.lang("This database uses outdated file links.") + "<br><br>"
                + Localization.lang("JabRef no longer supports 'ps' or 'pdf' fields.<br>File links are now stored in the 'file' field and files are stored in an external file directory.<br>To make use of this feature, JabRef needs to upgrade file links.<br><br>") + "<p>"
                + Localization.lang("Do you want JabRef to do the following operations?") + "</html>")).xy(1, row);

        if (offerChangeSettings) {
            formBuilder.appendRows("2dlu, p");
            row += 2;
            formBuilder.add(changeSettings).xy(1, row);
        }
        if (offerChangeDatabase) {
            formBuilder.appendRows("2dlu, p");
            row += 2;
            formBuilder.add(changeDatabase).xy(1, row);
        }
        if (offerSetFileDir) {
            if (Globals.prefs.hasKey("pdfDirectory")) {
                fileDir.setText(Globals.prefs.get("pdfDirectory"));
            } else {
                fileDir.setText(Globals.prefs.get("psDirectory"));
            }
            JPanel builderPanel = new JPanel();
            builderPanel.add(setFileDir);
            builderPanel.add(fileDir);
            JButton browse = new JButton(Localization.lang("Browse"));
            browse.addActionListener(BrowseAction.buildForDir(fileDir));
            builderPanel.add(browse);
            formBuilder.appendRows("2dlu, p");
            row += 2;
            formBuilder.add(builderPanel).xy(1, row);
        }
        formBuilder.appendRows("6dlu, p");
        formBuilder.add(doNotShowDialog).xy(1, row+2);

        message.add(formBuilder.build());

        int answer = JOptionPane.showConfirmDialog(panel.frame(),
                message, Localization.lang("Upgrade file"), JOptionPane.YES_NO_OPTION);
        if (doNotShowDialog.isSelected()) {
            Globals.prefs.putBoolean(JabRefPreferences.SHOW_FILE_LINKS_UPGRADE_WARNING, false);
        }

        if (answer == JOptionPane.YES_OPTION) {
            makeChanges(panel, parserResult, changeSettings.isSelected(), changeDatabase.isSelected(),
                    setFileDir.isSelected() ? fileDir.getText() : null);
        }
    }

    private boolean isThereSomethingToBeDone() {
        return  offerChangeSettings || offerChangeDatabase || offerSetFileDir;
    }

    /**
     * Check the database to find out whether any of a set of fields are used
     * for any of the entries.
     * @param database The bib database.
     * @param fields The set of fields to look for.
     * @return true if at least one of the given fields is set in at least one entry,
     *  false otherwise.
     */
    private boolean linksFound(BibDatabase database, String[] fields) {
        for (BibEntry entry : database.getEntries()) {
            for (String field : fields) {
                if (entry.hasField(field)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This method performs the actual changes.
     * @param panel
     * @param pr
     * @param fileDir The path to the file directory to set, or null if it should not be set.
     */
    private void makeChanges(BasePanel panel, ParserResult pr, boolean upgradePrefs,
                             boolean upgradeDatabase, String fileDir) {

        if (upgradeDatabase) {
            // Update file links links in the database:
            NamedCompound ce = upgradePdfPsToFile(pr.getDatabase(), FileLinksUpgradeWarning.FIELDS_TO_LOOK_FOR);
            panel.undoManager.addEdit(ce);
            panel.markBaseChanged();
        }

        if (fileDir != null) {
            Globals.prefs.put(Globals.FILE_FIELD + Globals.DIR_SUFFIX, fileDir);
        }

        if (upgradePrefs) {
            // Exchange table columns:
            Globals.prefs.putBoolean(JabRefPreferences.FILE_COLUMN, Boolean.TRUE);

            // Modify General fields if necessary:
            // If we don't find the file field, insert it at the bottom of the first tab:
            if (!showsFileInGenFields()) {
                String gfs = Globals.prefs.get(JabRefPreferences.CUSTOM_TAB_FIELDS + "0");
                StringBuilder sb = new StringBuilder(gfs);
                if (!gfs.isEmpty()) {
                    sb.append(';');
                }
                sb.append(Globals.FILE_FIELD);
                Globals.prefs.put(JabRefPreferences.CUSTOM_TAB_FIELDS + "0", sb.toString());
                Globals.prefs.updateEntryEditorTabList();
                panel.frame().removeCachedEntryEditors();
            }
            panel.frame().setupAllTables();
        }
    }

    private boolean showsFileInGenFields() {
        boolean found = false;
        EntryEditorTabList tabList = Globals.prefs.getEntryEditorTabList();
        outer: for (int i = 0; i < tabList.getTabCount(); i++) {
            List<String> fields = tabList.getTabFields(i);
            for (String field : fields) {
                if (field.equals(Globals.FILE_FIELD)) {
                    found = true;
                    break outer;
                }
            }
        }
        return found;
    }

    /**
     * Collect file links from the given set of fields, and add them to the list contained in the field
     * GUIGlobals.FILE_FIELD.
     *
     * @param database The database to modify.
     * @param fields   The fields to find links in.
     * @return A CompoundEdit specifying the undo operation for the whole operation.
     */
    private static NamedCompound upgradePdfPsToFile(BibDatabase database, String[] fields) {
        NamedCompound ce = new NamedCompound(Localization.lang("Move external links to 'file' field"));

        UpgradePdfPsToFileCleanup cleanupJob = new UpgradePdfPsToFileCleanup(Arrays.asList(fields));
        for (BibEntry entry : database.getEntries()) {
            List<FieldChange> changes = cleanupJob.cleanup(entry);

            for (FieldChange change : changes) {
                ce.addEdit(new UndoableFieldChange(change));
            }
        }

        ce.end();
        return ce;
    }
}
