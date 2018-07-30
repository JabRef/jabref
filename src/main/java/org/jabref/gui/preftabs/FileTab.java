package org.jabref.gui.preftabs;

import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.jabref.gui.DialogService;
import org.jabref.gui.customjfx.CustomJFXPanel;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.FieldName;
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.preferences.JabRefPreferences;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Preferences tab for file options. These options were moved out from GeneralTab to
 * resolve the space issue.
 */
class FileTab extends JPanel implements PrefsTab {

    private final DialogService dialogService;
    private final JabRefPreferences prefs;

    private final CheckBox backup;
    private final CheckBox localAutoSave;
    private final CheckBox openLast;
    private final ComboBox newlineSeparator;
    private final CheckBox reformatFileOnSaveAndExport;
    private final RadioButton resolveStringsStandard;
    private final RadioButton resolveStringsAll;
    private final TextField nonWrappableFields;
    private final TextField doNotResolveStringsFor;

    private final TextField fileDir;
    private final CheckBox bibLocAsPrimaryDir;
    private final CheckBox runAutoFileSearch;
    private final CheckBox allowFileAutoOpenBrowse;
    private final RadioButton useRegExpComboBox;
    private final RadioButton matchExactKeyOnly = new RadioButton(
            Localization.lang("Autolink only files that match the BibTeX key"));
    private final RadioButton matchStartsWithKey = new RadioButton(
            Localization.lang("Autolink files with names starting with the BibTeX key"));
    private final TextField regExpTextField;

    public FileTab(DialogService dialogService, JabRefPreferences prefs) {
        this.dialogService = dialogService;
        this.prefs = prefs;

        fileDir = new TextField();
        bibLocAsPrimaryDir = new CheckBox(Localization.lang("Use the BIB file location as primary file directory"));
        bibLocAsPrimaryDir.setText(Localization.lang("When downloading files, or moving linked files to the "
                + "file directory, prefer the BIB file location rather than the file directory set above"));
        runAutoFileSearch = new CheckBox(
                Localization.lang("When opening file link, search for matching file if no link is defined"));
        allowFileAutoOpenBrowse = new CheckBox(
                Localization.lang("Automatically open browse dialog when creating new file link"));
        regExpTextField = new TextField();
        useRegExpComboBox = new RadioButton(Localization.lang("Use regular expression search"));
        useRegExpComboBox.setOnAction(e -> regExpTextField.setEditable(useRegExpComboBox.isSelected()));


        openLast = new CheckBox(Localization.lang("Open last edited libraries at startup"));
        backup = new CheckBox(Localization.lang("Backup old file when saving"));
        localAutoSave = new CheckBox(Localization.lang("Autosave local libraries"));
        resolveStringsAll = new RadioButton(Localization.lang("Resolve strings for all fields except") + ":");
        resolveStringsStandard = new RadioButton(Localization.lang("Resolve strings for standard BibTeX fields only"));

        // This is sort of a quick hack
        newlineSeparator = new ComboBox<>(FXCollections.observableArrayList("CR", "CR/LF", "LF"));

        reformatFileOnSaveAndExport = new CheckBox(Localization.lang("Always reformat BIB file on save and export"));

        nonWrappableFields = new TextField();
        doNotResolveStringsFor = new TextField();

        GridPane builder = new GridPane();

        builder.add(new Label(Localization.lang("General")),1,1);
        builder.add(openLast, 1,2);
        builder.add(backup,1, 3);


        Label label = new Label(Localization.lang("Do not wrap the following fields when saving") + ":");
        builder.add(label,1,4);
        builder.add(nonWrappableFields,2,4);

        builder.add(resolveStringsStandard, 1,5);
        builder.add(resolveStringsAll,1,6);
        builder.add(doNotResolveStringsFor,2,6);


        Label lab = new Label(Localization.lang("Newline separator") + ":");
        builder.add(lab,1,7);
        builder.add(newlineSeparator,2,7);


        builder.add(reformatFileOnSaveAndExport, 1,8);


        builder.add(new Label(Localization.lang("External file links")),1,10);

        lab = new Label(Localization.lang("Main file directory") + ':');
        builder.add(lab,1,11);
        builder.add(fileDir,2,11);

        Button browse = new Button(Localization.lang("Browse"));
        browse.setOnAction(e -> {

            DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                    .withInitialDirectory(Paths.get(fileDir.getText())).build();

            DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showDirectorySelectionDialog(dirDialogConfiguration))
                    .ifPresent(f -> fileDir.setText(f.toString()));

        });
        builder.add(browse,3,11);


        builder.add(bibLocAsPrimaryDir, 1,12);
        builder.add(matchStartsWithKey, 1,13);
        builder.add(matchExactKeyOnly, 1,14);
        builder.add(useRegExpComboBox,1,15);
        builder.add(regExpTextField,2,15);

        Button help = new Button("?");
        help.setOnAction(event -> new HelpAction(Localization.lang("Help on regular expression search"),
                HelpFile.REGEX_SEARCH).getHelpButton().doClick());

        builder.add(help,3,15);

        builder.add(runAutoFileSearch, 1,16);
        builder.add(allowFileAutoOpenBrowse,1,17);


        builder.add(new Label(Localization.lang("Autosave")),1,18);
        builder.add(localAutoSave, 1,19);
        Button help1 = new Button("?");
        help1.setOnAction(event -> new HelpAction(HelpFile.AUTOSAVE).getHelpButton().doClick());
        builder.add(help1,2,19);


        JFXPanel panel = CustomJFXPanel.wrap(new Scene(builder));
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        fileDir.setText(prefs.get(FieldName.FILE + FileDirectoryPreferences.DIR_SUFFIX));
        bibLocAsPrimaryDir.setSelected(prefs.getBoolean(JabRefPreferences.BIB_LOC_AS_PRIMARY_DIR));
        runAutoFileSearch.setSelected(prefs.getBoolean(JabRefPreferences.RUN_AUTOMATIC_FILE_SEARCH));
        allowFileAutoOpenBrowse.setSelected(prefs.getBoolean(JabRefPreferences.ALLOW_FILE_AUTO_OPEN_BROWSE));
        regExpTextField.setText(prefs.get(JabRefPreferences.AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY));
        if (prefs.getBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY)) {
            useRegExpComboBox.setSelected(true);
        } else if (prefs.getBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY)) {
            matchExactKeyOnly.setSelected(true);
        } else {
            matchStartsWithKey.setSelected(true);
        }

        openLast.setSelected(prefs.getBoolean(JabRefPreferences.OPEN_LAST_EDITED));
        backup.setSelected(prefs.getBoolean(JabRefPreferences.BACKUP));

        String newline = prefs.get(JabRefPreferences.NEWLINE);
        if ("\r".equals(newline)) {
            newlineSeparator.setValue("CR");
        } else if ("\n".equals(newline)) {
            newlineSeparator.setValue("LF");
        } else {
            // fallback: windows standard
            newlineSeparator.setValue("CR/LF");
        }
        reformatFileOnSaveAndExport.setSelected(prefs.getBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT));

        resolveStringsAll.setSelected(prefs.getBoolean(JabRefPreferences.RESOLVE_STRINGS_ALL_FIELDS));
        resolveStringsStandard.setSelected(!resolveStringsAll.isSelected());
        doNotResolveStringsFor.setText(prefs.get(JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR));
        nonWrappableFields.setText(prefs.get(JabRefPreferences.NON_WRAPPABLE_FIELDS));

        localAutoSave.setSelected(prefs.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE));
    }

    @Override
    public void storeSettings() {
        prefs.put(FieldName.FILE + FileDirectoryPreferences.DIR_SUFFIX, fileDir.getText());
        prefs.putBoolean(JabRefPreferences.BIB_LOC_AS_PRIMARY_DIR, bibLocAsPrimaryDir.isSelected());
        prefs.putBoolean(JabRefPreferences.RUN_AUTOMATIC_FILE_SEARCH, runAutoFileSearch.isSelected());
        prefs.putBoolean(JabRefPreferences.ALLOW_FILE_AUTO_OPEN_BROWSE, allowFileAutoOpenBrowse.isSelected());
        prefs.putBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY, useRegExpComboBox.isSelected());
        prefs.putBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY, matchExactKeyOnly.isSelected());
        if (useRegExpComboBox.isSelected()) {
            prefs.put(JabRefPreferences.AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY, regExpTextField.getText());
        }

        String newline;
        switch (newlineSeparator.getPromptText()) {
            case "CR":
                newline = "\r";
                break;
            case "LF":
                newline = "\n";
                break;
            default:
                newline = "\r\n";
                break;
        }
        prefs.put(JabRefPreferences.NEWLINE, newline);
        // we also have to change Globals variable as globals is not a getter, but a constant
        OS.NEWLINE = newline;

        prefs.putBoolean(JabRefPreferences.BACKUP, backup.isSelected());

        prefs.putBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT, reformatFileOnSaveAndExport.isSelected());
        prefs.putBoolean(JabRefPreferences.OPEN_LAST_EDITED, openLast.isSelected());
        prefs.putBoolean(JabRefPreferences.RESOLVE_STRINGS_ALL_FIELDS, resolveStringsAll.isSelected());
        prefs.put(JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR, doNotResolveStringsFor.getText().trim());
        doNotResolveStringsFor.setText(prefs.get(JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR));

        if (!nonWrappableFields.getText().trim().equals(prefs.get(JabRefPreferences.NON_WRAPPABLE_FIELDS))) {
            prefs.put(JabRefPreferences.NON_WRAPPABLE_FIELDS, nonWrappableFields.getText());
        }

        prefs.putBoolean(JabRefPreferences.LOCAL_AUTO_SAVE, localAutoSave.isSelected());
    }

    @Override
    public boolean validateSettings() {
        Path path = Paths.get(fileDir.getText());
        boolean valid = Files.exists(path) && Files.isDirectory(path);
        if (!valid) {
            dialogService.showErrorDialogAndWait(
                    String.format("%s -> %s %n %n %s: %n %s", Localization.lang("File"),
                            Localization.lang("Main file directory"), Localization.lang("Directory not found"), path));
        }
        return valid;
    }

    @Override
    public String getTabName() {
        return Localization.lang("File");
    }

}
