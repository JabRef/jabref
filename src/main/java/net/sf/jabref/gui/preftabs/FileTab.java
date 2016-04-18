/*  Copyright (C) 2003-2016 JabRef contributors.
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
package net.sf.jabref.gui.preftabs;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.BrowseAction;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemListener;

/**
 * Preferences tab for file options. These options were moved out from GeneralTab to
 * resolve the space issue.
 */
class FileTab extends JPanel implements PrefsTab {

    private final JabRefPreferences prefs;
    private final JabRefFrame frame;

    private final JCheckBox backup;
    private final JCheckBox openLast;
    private final JCheckBox autoSave;
    private final JCheckBox promptBeforeUsingAutoSave;
    private final JComboBox<String> valueDelimiter;
    private final JComboBox<String> newlineSeparator;
    private final JCheckBox reformatFileOnSaveAndExport;
    private final JRadioButton resolveStringsStandard;
    private final JRadioButton resolveStringsAll;
    private final JTextField bracesAroundCapitalsFields;
    private final JTextField nonWrappableFields;
    private final JTextField doNotResolveStringsFor;
    private final JSpinner autoSaveInterval;
    private boolean origAutoSaveSetting;

    private final JTextField fileDir;
    private final JCheckBox bibLocationAsFileDir;
    private final JCheckBox bibLocAsPrimaryDir;
    private final JCheckBox runAutoFileSearch;
    private final JCheckBox allowFileAutoOpenBrowse;
    private final JRadioButton useRegExpComboBox;
    private final JRadioButton matchExactKeyOnly = new JRadioButton(Localization.lang("Autolink only files that match the BibTeX key"));
    private final JRadioButton matchStartsWithKey = new JRadioButton(Localization.lang("Autolink files with names starting with the BibTeX key"));
    private final JTextField regExpTextField;


    public FileTab(JabRefFrame frame, JabRefPreferences prefs) {
        this.prefs = prefs;
        this.frame = frame;

        fileDir = new JTextField(25);
        bibLocationAsFileDir = new JCheckBox(Localization.lang("Allow file links relative to each bib file's location"));
        bibLocAsPrimaryDir = new JCheckBox(Localization.lang("Use the bib file location as primary file directory"));
        bibLocAsPrimaryDir.setToolTipText(Localization.lang("When downloading files, or moving linked files to the "
                + "file directory, prefer the bib file location rather than the file directory set above"));
        bibLocationAsFileDir.addChangeListener(e -> bibLocAsPrimaryDir.setEnabled(bibLocationAsFileDir.isSelected()));
        runAutoFileSearch = new JCheckBox(Localization.lang("When opening file link, search for matching file if no link is defined"));
        allowFileAutoOpenBrowse = new JCheckBox(Localization.lang("Automatically open browse dialog when creating new file link"));
        regExpTextField = new JTextField(25);
        useRegExpComboBox = new JRadioButton(Localization.lang("Use Regular Expression Search"));
        ItemListener regExpListener = e -> regExpTextField.setEditable(useRegExpComboBox.isSelected());
        useRegExpComboBox.addItemListener(regExpListener);
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(matchExactKeyOnly);
        buttonGroup.add(matchStartsWithKey);
        buttonGroup.add(useRegExpComboBox);

        openLast = new JCheckBox(Localization.lang("Open last edited databases at startup"));
        backup = new JCheckBox(Localization.lang("Backup old file when saving"));
        autoSave = new JCheckBox(Localization.lang("Autosave"));
        promptBeforeUsingAutoSave = new JCheckBox(Localization.lang("Prompt before recovering a database from an autosave file"));
        autoSaveInterval = new JSpinner(new SpinnerNumberModel(1, 1, 60, 1));
        valueDelimiter = new JComboBox<>(new String[] {
                Localization.lang("Quotes") + ": \", \"",
                Localization.lang("Curly Brackets") + ": {, }"});
        resolveStringsAll = new JRadioButton(Localization.lang("Resolve strings for all fields except") + ":");
        resolveStringsStandard = new JRadioButton(Localization.lang("Resolve strings for standard BibTeX fields only"));
        ButtonGroup bg = new ButtonGroup();
        bg.add(resolveStringsAll);
        bg.add(resolveStringsStandard);

        // This is sort of a quick hack
        newlineSeparator = new JComboBox<>(new String[] {"CR", "CR/LF", "LF"});

        reformatFileOnSaveAndExport = new JCheckBox(Localization.lang("Always reformat .bib file on save and export"));

        bracesAroundCapitalsFields = new JTextField(25);
        nonWrappableFields = new JTextField(25);
        doNotResolveStringsFor = new JTextField(30);

        autoSave.addChangeListener(e -> {
            autoSaveInterval.setEnabled(autoSave.isSelected());
            promptBeforeUsingAutoSave.setEnabled(autoSave.isSelected());
        });

        FormLayout layout = new FormLayout("left:pref, 4dlu, fill:150dlu, 4dlu, fill:pref", ""); // left:pref, 4dlu, fill:pref
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.appendSeparator(Localization.lang("General"));
        builder.nextLine();
        builder.append(openLast, 3);
        builder.nextLine();
        builder.append(backup, 3);
        builder.nextLine();

        JLabel label = new JLabel(Localization.lang("Store the following fields with braces around capital letters") + ":");
        builder.append(label);
        builder.append(bracesAroundCapitalsFields);
        builder.nextLine();
        label = new JLabel(Localization.lang("Do not wrap the following fields when saving") + ":");
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
        BrowseAction browse = BrowseAction.buildForDir(this.frame, fileDir);
        builder.append(new JButton(browse));
        builder.nextLine();
        builder.append(bibLocationAsFileDir, 3);
        builder.nextLine();
        builder.append(bibLocAsPrimaryDir, 3);
        builder.nextLine();
        builder.append(matchStartsWithKey, 3);
        builder.nextLine();
        builder.append(matchExactKeyOnly, 3);
        builder.nextLine();
        builder.append(useRegExpComboBox);
        builder.append(regExpTextField);

        builder.append(new HelpAction(Localization.lang("Help on Regular Expression Search"), HelpFiles.regularExpressionSearchHelp).getHelpButton());
        builder.nextLine();
        builder.append(runAutoFileSearch, 3);
        builder.nextLine();
        builder.append(allowFileAutoOpenBrowse);
        builder.nextLine();

        builder.appendSeparator(Localization.lang("Autosave"));
        builder.append(autoSave, 1);
        JButton help = new HelpAction(HelpFiles.autosaveHelp).getHelpButton();
        help.setPreferredSize(new Dimension(24, 24));
        JPanel hPan = new JPanel();
        hPan.setLayout(new BorderLayout());
        hPan.add(help, BorderLayout.EAST);
        builder.append(hPan);
        builder.nextLine();
        builder.append(Localization.lang("Autosave interval (minutes)") + ":");
        builder.append(autoSaveInterval);
        builder.nextLine();
        builder.append(promptBeforeUsingAutoSave);
        builder.nextLine();

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout());
        add(pan, BorderLayout.CENTER);
    }


    @Override
    public void setValues() {
        fileDir.setText(prefs.get(Globals.FILE_FIELD + Globals.DIR_SUFFIX));
        bibLocAsPrimaryDir.setSelected(prefs.getBoolean(JabRefPreferences.BIB_LOC_AS_PRIMARY_DIR));
        bibLocAsPrimaryDir.setEnabled(bibLocationAsFileDir.isSelected());
        runAutoFileSearch.setSelected(prefs.getBoolean(JabRefPreferences.RUN_AUTOMATIC_FILE_SEARCH));
        allowFileAutoOpenBrowse.setSelected(prefs.getBoolean(JabRefPreferences.ALLOW_FILE_AUTO_OPEN_BROWSE));
        regExpTextField.setText(prefs.get(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY));
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
        bracesAroundCapitalsFields.setText(prefs.get(JabRefPreferences.PUT_BRACES_AROUND_CAPITALS));
        nonWrappableFields.setText(prefs.get(JabRefPreferences.NON_WRAPPABLE_FIELDS));

        autoSave.setSelected(prefs.getBoolean(JabRefPreferences.AUTO_SAVE));
        promptBeforeUsingAutoSave.setSelected(prefs.getBoolean(JabRefPreferences.PROMPT_BEFORE_USING_AUTOSAVE));
        autoSaveInterval.setValue(prefs.getInt(JabRefPreferences.AUTO_SAVE_INTERVAL));
        origAutoSaveSetting = autoSave.isSelected();
        valueDelimiter.setSelectedIndex(prefs.getInt(JabRefPreferences.VALUE_DELIMITERS2));
    }

    @Override
    public void storeSettings() {
        prefs.put(Globals.FILE_FIELD + Globals.DIR_SUFFIX, fileDir.getText());
        prefs.putBoolean(JabRefPreferences.BIB_LOC_AS_PRIMARY_DIR, bibLocAsPrimaryDir.isSelected());
        prefs.putBoolean(JabRefPreferences.RUN_AUTOMATIC_FILE_SEARCH, runAutoFileSearch.isSelected());
        prefs.putBoolean(JabRefPreferences.ALLOW_FILE_AUTO_OPEN_BROWSE, allowFileAutoOpenBrowse.isSelected());
        prefs.putBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY, useRegExpComboBox.isSelected());
        prefs.putBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY, matchExactKeyOnly.isSelected());
        if (useRegExpComboBox.isSelected()) {
            prefs.put(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY, regExpTextField.getText());
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
        Globals.NEWLINE = newline;

        prefs.putBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT, reformatFileOnSaveAndExport.isSelected());
        prefs.putBoolean(JabRefPreferences.BACKUP, backup.isSelected());
        prefs.putBoolean(JabRefPreferences.OPEN_LAST_EDITED, openLast.isSelected());
        prefs.putBoolean(JabRefPreferences.RESOLVE_STRINGS_ALL_FIELDS, resolveStringsAll.isSelected());
        prefs.put(JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR, doNotResolveStringsFor.getText().trim());
        prefs.putBoolean(JabRefPreferences.AUTO_SAVE, autoSave.isSelected());
        prefs.putBoolean(JabRefPreferences.PROMPT_BEFORE_USING_AUTOSAVE, promptBeforeUsingAutoSave.isSelected());
        prefs.putInt(JabRefPreferences.AUTO_SAVE_INTERVAL, (Integer) autoSaveInterval.getValue());
        prefs.putInt(JabRefPreferences.VALUE_DELIMITERS2, valueDelimiter.getSelectedIndex());
        doNotResolveStringsFor.setText(prefs.get(JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR));

        boolean updateSpecialFields = false;
        if (!bracesAroundCapitalsFields.getText().trim().equals(prefs.get(JabRefPreferences.PUT_BRACES_AROUND_CAPITALS))) {
            prefs.put(JabRefPreferences.PUT_BRACES_AROUND_CAPITALS, bracesAroundCapitalsFields.getText());
            updateSpecialFields = true;
        }
        if (!nonWrappableFields.getText().trim().equals(prefs.get(JabRefPreferences.NON_WRAPPABLE_FIELDS))) {
            prefs.put(JabRefPreferences.NON_WRAPPABLE_FIELDS, nonWrappableFields.getText());
            updateSpecialFields = true;
        }
        // If either of the two last entries were changed, run the update for special field handling:
        if (updateSpecialFields) {
            prefs.updateSpecialFieldHandling();
        }

        // See if we should start or stop the auto save manager:
        if (!origAutoSaveSetting && autoSave.isSelected()) {
            Globals.startAutoSaveManager(frame);
        }
        else if (origAutoSaveSetting && !autoSave.isSelected()) {
            Globals.stopAutoSaveManager();
        }

    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("File");
    }

}
