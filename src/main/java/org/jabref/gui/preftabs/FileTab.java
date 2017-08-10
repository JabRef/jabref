package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.FieldName;
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Preferences tab for file options. These options were moved out from GeneralTab to
 * resolve the space issue.
 */
class FileTab extends JPanel implements PrefsTab {

    private final JabRefPreferences prefs;
    private final JabRefFrame frame;

    private final JCheckBox backup;
    private final JCheckBox localAutoSave;
    private final JCheckBox openLast;
    private final JComboBox<String> newlineSeparator;
    private final JCheckBox reformatFileOnSaveAndExport;
    private final JRadioButton resolveStringsStandard;
    private final JRadioButton resolveStringsAll;
    private final JTextField nonWrappableFields;
    private final JTextField doNotResolveStringsFor;

    private final JTextField fileDir;
    private final JCheckBox bibLocAsPrimaryDir;
    private final JCheckBox runAutoFileSearch;
    private final JCheckBox allowFileAutoOpenBrowse;
    private final JRadioButton useRegExpComboBox;
    private final JRadioButton matchExactKeyOnly = new JRadioButton(
            Localization.lang("Autolink only files that match the BibTeX key"));
    private final JRadioButton matchStartsWithKey = new JRadioButton(
            Localization.lang("Autolink files with names starting with the BibTeX key"));
    private final JTextField regExpTextField;

    public FileTab(JabRefFrame frame, JabRefPreferences prefs) {
        this.prefs = prefs;
        this.frame = frame;

        fileDir = new JTextField(25);
        bibLocAsPrimaryDir = new JCheckBox(Localization.lang("Use the BIB file location as primary file directory"));
        bibLocAsPrimaryDir.setToolTipText(Localization.lang("When downloading files, or moving linked files to the "
                + "file directory, prefer the BIB file location rather than the file directory set above"));
        runAutoFileSearch = new JCheckBox(
                Localization.lang("When opening file link, search for matching file if no link is defined"));
        allowFileAutoOpenBrowse = new JCheckBox(
                Localization.lang("Automatically open browse dialog when creating new file link"));
        regExpTextField = new JTextField(25);
        useRegExpComboBox = new JRadioButton(Localization.lang("Use regular expression search"));
        ItemListener regExpListener = e -> regExpTextField.setEditable(useRegExpComboBox.isSelected());
        useRegExpComboBox.addItemListener(regExpListener);
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(matchExactKeyOnly);
        buttonGroup.add(matchStartsWithKey);
        buttonGroup.add(useRegExpComboBox);

        openLast = new JCheckBox(Localization.lang("Open last edited libraries at startup"));
        backup = new JCheckBox(Localization.lang("Backup old file when saving"));
        localAutoSave = new JCheckBox(Localization.lang("Autosave local libraries"));
        resolveStringsAll = new JRadioButton(Localization.lang("Resolve strings for all fields except") + ":");
        resolveStringsStandard = new JRadioButton(Localization.lang("Resolve strings for standard BibTeX fields only"));
        ButtonGroup bg = new ButtonGroup();
        bg.add(resolveStringsAll);
        bg.add(resolveStringsStandard);

        // This is sort of a quick hack
        newlineSeparator = new JComboBox<>(new String[] {"CR", "CR/LF", "LF"});

        reformatFileOnSaveAndExport = new JCheckBox(Localization.lang("Always reformat BIB file on save and export"));

        nonWrappableFields = new JTextField(25);
        doNotResolveStringsFor = new JTextField(30);

        FormLayout layout = new FormLayout("left:pref, 4dlu, fill:150dlu, 4dlu, fill:pref", ""); // left:pref, 4dlu, fill:pref
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.appendSeparator(Localization.lang("General"));
        builder.nextLine();
        builder.append(openLast, 3);
        builder.nextLine();
        builder.append(backup, 3);
        builder.nextLine();

        JLabel label = new JLabel(Localization.lang("Do not wrap the following fields when saving") + ":");
        builder.append(label);
        builder.append(nonWrappableFields);
        builder.nextLine();
        builder.append(resolveStringsStandard, 3);
        builder.nextLine();
        builder.append(resolveStringsAll);
        builder.append(doNotResolveStringsFor);
        builder.nextLine();

        JLabel lab = new JLabel(Localization.lang("Newline separator") + ":");
        builder.append(lab);
        builder.append(newlineSeparator);
        builder.nextLine();

        builder.append(reformatFileOnSaveAndExport, 3);
        builder.nextLine();

        builder.appendSeparator(Localization.lang("External file links"));
        builder.nextLine();
        lab = new JLabel(Localization.lang("Main file directory") + ':');
        builder.append(lab);
        builder.append(fileDir);

        JButton browse = new JButton(Localization.lang("Browse"));
        browse.addActionListener(e -> {

            DialogService ds = new FXDialogService();
            DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                    .withInitialDirectory(Paths.get(fileDir.getText())).build();

            DefaultTaskExecutor.runInJavaFXThread(() -> ds.showDirectorySelectionDialog(dirDialogConfiguration))
                    .ifPresent(f -> fileDir.setText(f.toString()));

        });
        builder.append(browse);

        builder.nextLine();
        builder.append(bibLocAsPrimaryDir, 3);
        builder.nextLine();
        builder.append(matchStartsWithKey, 3);
        builder.nextLine();
        builder.append(matchExactKeyOnly, 3);
        builder.nextLine();
        builder.append(useRegExpComboBox);
        builder.append(regExpTextField);

        builder.append(new HelpAction(Localization.lang("Help on regular expression search"),
                HelpFile.REGEX_SEARCH)
                        .getHelpButton());
        builder.nextLine();
        builder.append(runAutoFileSearch, 3);
        builder.nextLine();
        builder.append(allowFileAutoOpenBrowse);
        builder.nextLine();

        builder.appendSeparator(Localization.lang("Autosave"));
        builder.append(localAutoSave, 1);
        JButton help = new HelpAction(HelpFile.AUTOSAVE).getHelpButton();
        help.setPreferredSize(new Dimension(24, 24));
        JPanel hPan = new JPanel();
        hPan.setLayout(new BorderLayout());
        hPan.add(help, BorderLayout.EAST);
        builder.append(hPan);
        builder.nextLine();

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout());
        add(pan, BorderLayout.CENTER);
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
            newlineSeparator.setSelectedIndex(0);
        } else if ("\n".equals(newline)) {
            newlineSeparator.setSelectedIndex(2);
        } else {
            // fallback: windows standard
            newlineSeparator.setSelectedIndex(1);
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
        switch (newlineSeparator.getSelectedIndex()) {
        case 0:
            newline = "\r";
            break;
        case 2:
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
            String content = String.format("%s -> %s %n %n %s: %n %s", Localization.lang("File"),
                    Localization.lang("Main file directory"), Localization.lang("Directory not found"), path);
            JOptionPane.showMessageDialog(this.frame, content, Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
        }
        return valid;
    }

    @Override
    public String getTabName() {
        return Localization.lang("File");
    }

}
