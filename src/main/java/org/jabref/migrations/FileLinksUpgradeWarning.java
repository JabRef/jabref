package org.jabref.migrations;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.entryeditor.EntryEditorTabList;
import org.jabref.gui.importer.actions.GUIPostOpenAction;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.cleanup.UpgradePdfPsToFileCleanup;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This class defines the warning that can be offered when opening a pre-2.3
 * JabRef file into a later version. This warning mentions the new external file
 * link system in this version of JabRef, and offers to:
 *
 * * upgrade old-style PDF/PS links into the "file" field
 * * modify General fields to show "file" instead of "pdf" / "ps"
 * * modify table column settings to show "file" instead of "pdf" / "ps"
 */
public class FileLinksUpgradeWarning implements GUIPostOpenAction {

    private static final String[] FIELDS_TO_LOOK_FOR = new String[]{FieldName.PDF, FieldName.PS};

    private boolean offerChangeSettings;

    private boolean offerChangeDatabase;

    private boolean offerSetFileDir;

    /**
     * Collect file links from the given set of fields, and add them to the list contained in the field
     * GUIGlobals.FILE_FIELD.
     *
     * @param database The database to modify.
     * @return A CompoundEdit specifying the undo operation for the whole operation.
     */
    private static NamedCompound upgradePdfPsToFile(BibDatabase database) {
        NamedCompound ce = new NamedCompound(Localization.lang("Move external links to 'file' field"));

        UpgradePdfPsToFileCleanup cleanupJob = new UpgradePdfPsToFileCleanup();
        for (BibEntry entry : database.getEntries()) {
            List<FieldChange> changes = cleanupJob.cleanup(entry);

            for (FieldChange change : changes) {
                ce.addEdit(new UndoableFieldChange(change));
            }
        }

        ce.end();
        return ce;
    }

    /**
     * This method should be performed if the major/minor versions recorded in the ParserResult
     * are less than or equal to 2.2.
     *
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
        offerSetFileDir = !Globals.prefs.hasKey(FieldName.FILE + FileDirectoryPreferences.DIR_SUFFIX)
                && (Globals.prefs.hasKey(FieldName.PDF + FileDirectoryPreferences.DIR_SUFFIX)
                || Globals.prefs.hasKey(FieldName.PS + FileDirectoryPreferences.DIR_SUFFIX));

        // First check if this warning is disabled:
        return Globals.prefs.getBoolean(JabRefPreferences.SHOW_FILE_LINKS_UPGRADE_WARNING)
                && isThereSomethingToBeDone();
    }

    /*
     * This method presents a dialog box explaining and offering to make the
     * changes. If the user confirms, the changes are performed.
     */
    @Override
    public void performAction(BasePanel panel, ParserResult parserResult) {

        if (!isThereSomethingToBeDone()) {
            return; // Nothing to do, just return.
        }

        JCheckBox changeSettings = new JCheckBox(
                Localization.lang("Change table column and General fields settings to use the new feature"),
                offerChangeSettings);
        JCheckBox changeDatabase = new JCheckBox(
                Localization.lang("Upgrade old external file links to use the new feature"),
                offerChangeDatabase);
        JCheckBox setFileDir = new JCheckBox(Localization.lang("Set main external file directory") + ":",
                offerSetFileDir);
        JTextField fileDir = new JTextField(30);
        JCheckBox doNotShowDialog = new JCheckBox(Localization.lang("Do not show these options in the future"),
                false);

        JPanel message = new JPanel();
        FormBuilder formBuilder = FormBuilder.create().layout(new FormLayout("left:pref", "p"));
        // Keep the formatting of these lines. Otherwise, strings have to be translated again.
        // See updated JabRef_en.properties modifications by python syncLang.py -s -u
        int row = 1;
        formBuilder.add(new JLabel("<html>" + Localization.lang("This library uses outdated file links.") + "<br><br>"
                + Localization
                .lang("JabRef no longer supports 'ps' or 'pdf' fields.<br>File links are now stored in the 'file' field and files are stored in an external file directory.<br>To make use of this feature, JabRef needs to upgrade file links.<br><br>")
                + "<p>"
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
            if (Globals.prefs.hasKey(FieldName.PDF + FileDirectoryPreferences.DIR_SUFFIX)) {
                fileDir.setText(Globals.prefs.get(FieldName.PDF + FileDirectoryPreferences.DIR_SUFFIX));
            } else {
                fileDir.setText(Globals.prefs.get(FieldName.PS + FileDirectoryPreferences.DIR_SUFFIX));
            }
            JPanel builderPanel = new JPanel();
            builderPanel.add(setFileDir);
            builderPanel.add(fileDir);
            JButton browse = new JButton(Localization.lang("Browse"));

            FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                    .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();
            DialogService ds = new FXDialogService();

            browse.addActionListener(
                    e -> DefaultTaskExecutor.runInJavaFXThread(() -> ds.showFileOpenDialog(fileDialogConfiguration)
                            .ifPresent(f -> fileDir.setText(f.toAbsolutePath().toString()))));
            builderPanel.add(browse);
            formBuilder.appendRows("2dlu, p");
            row += 2;
            formBuilder.add(builderPanel).xy(1, row);
        }
        formBuilder.appendRows("6dlu, p");
        formBuilder.add(doNotShowDialog).xy(1, row + 2);

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
        return offerChangeSettings || offerChangeDatabase || offerSetFileDir;
    }

    /**
     * Check the database to find out whether any of a set of fields are used
     * for any of the entries.
     *
     * @param database The BIB database.
     * @param fields   The set of fields to look for.
     * @return true if at least one of the given fields is set in at least one entry,
     * false otherwise.
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
     *
     * @param fileDir The path to the file directory to set, or null if it should not be set.
     */
    private void makeChanges(BasePanel panel, ParserResult pr, boolean upgradePrefs,
                             boolean upgradeDatabase, String fileDir) {

        if (upgradeDatabase) {
            // Update file links links in the database:
            NamedCompound ce = upgradePdfPsToFile(pr.getDatabase());
            panel.getUndoManager().addEdit(ce);
            panel.markBaseChanged();
        }

        if (fileDir != null) {
            Globals.prefs.put(FieldName.FILE + FileDirectoryPreferences.DIR_SUFFIX, fileDir);
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
                sb.append(FieldName.FILE);
                Globals.prefs.put(JabRefPreferences.CUSTOM_TAB_FIELDS + "0", sb.toString());
                Globals.prefs.updateEntryEditorTabList();
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
                if (field.equals(FieldName.FILE)) {
                    found = true;
                    break outer;
                }
            }
        }
        return found;
    }
}
