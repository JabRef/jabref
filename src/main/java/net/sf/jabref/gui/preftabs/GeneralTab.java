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
package net.sf.jabref.gui.preftabs;

import java.awt.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;

import javax.swing.*;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.logic.l10n.Encodings;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabaseMode;

import static net.sf.jabref.logic.l10n.Languages.LANGUAGES;

class GeneralTab extends JPanel implements PrefsTab {

    private final JCheckBox defSort;
    private final JCheckBox ctrlClick;
    private final JCheckBox useOwner;
    private final JCheckBox overwriteOwner;
    private final JCheckBox keyDuplicateWarningDialog;
    private final JCheckBox keyEmptyWarningDialog;
    private final JCheckBox enforceLegalKeys;
    private final JCheckBox confirmDelete;
    private final JCheckBox allowEditing;
    private final JCheckBox memoryStick;
    private final JCheckBox inspectionWarnDupli;
    private final JCheckBox useTimeStamp;
    private final JCheckBox updateTimeStamp;
    private final JCheckBox overwriteTimeStamp;
    private final JCheckBox markImportedEntries;
    private final JCheckBox unmarkAllEntriesBeforeImporting;
    private final JTextField defOwnerField;

    private final JTextField timeStampFormat;
    private final JTextField timeStampField;
    private final JabRefPreferences prefs;
    private final JComboBox<String> language = new JComboBox<>(LANGUAGES.keySet().toArray(new String[LANGUAGES.keySet().size()]));
    private final JComboBox<Charset> encodings;
    private final JComboBox<BibDatabaseMode> biblatexMode;

    public class DefaultBibModeRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setText(((BibDatabaseMode) value).getFormattedName());
            return this;
        }
    }


    public GeneralTab(JabRefPreferences prefs) {
        this.prefs = prefs;
        setLayout(new BorderLayout());

        biblatexMode = new JComboBox<>(BibDatabaseMode.values());
        biblatexMode.setRenderer(new DefaultBibModeRenderer());
        allowEditing = new JCheckBox(Localization.lang("Allow editing in table cells"));

        memoryStick = new JCheckBox(Localization.lang("Load and Save preferences from/to jabref.xml on start-up (memory stick mode)"));
        defSort = new JCheckBox(Localization.lang("Sort automatically"));
        ctrlClick = new JCheckBox(Localization.lang("Open right-click menu with Ctrl+left button"));
        useOwner = new JCheckBox(Localization.lang("Mark new entries with owner name") + ':');
        updateTimeStamp = new JCheckBox(Localization.lang("Update timestamp on modification"));
        useTimeStamp = new JCheckBox(Localization.lang("Mark new entries with addition date") + ". "
                + Localization.lang("Date format") + ':');
        useTimeStamp.addChangeListener(e -> updateTimeStamp.setEnabled(useTimeStamp.isSelected()));
        overwriteOwner = new JCheckBox(Localization.lang("Overwrite"));
        overwriteTimeStamp = new JCheckBox(Localization.lang("Overwrite"));
        overwriteOwner.setToolTipText(Localization.lang("If a pasted or imported entry already has "
                + "the field set, overwrite."));
        overwriteTimeStamp.setToolTipText(Localization.lang("If a pasted or imported entry already has "
                + "the field set, overwrite."));
        keyDuplicateWarningDialog = new JCheckBox(Localization.lang("Show warning dialog when a duplicate BibTeX key is entered"));
        keyEmptyWarningDialog = new JCheckBox(Localization.lang("Show warning dialog when an empty BibTeX key is entered"));
        enforceLegalKeys = new JCheckBox(Localization.lang("Enforce legal characters in BibTeX keys"));
        confirmDelete = new JCheckBox(Localization.lang("Show confirmation dialog when deleting entries"));

        markImportedEntries = new JCheckBox(Localization.lang("Mark entries imported into an existing database"));
        unmarkAllEntriesBeforeImporting = new JCheckBox(Localization.lang("Unmark all entries before importing new entries into an existing database"));
        defOwnerField = new JTextField();
        timeStampFormat = new JTextField();
        timeStampField = new JTextField();
        inspectionWarnDupli = new JCheckBox(Localization.lang("Warn about unresolved duplicates when closing inspection window"));

        encodings = new JComboBox<>();
        encodings.setModel(new DefaultComboBoxModel<>(Encodings.ENCODINGS));

        FormLayout layout = new FormLayout
                ("8dlu, 1dlu, left:170dlu, 4dlu, fill:pref, 4dlu, fill:pref, 4dlu, left:pref, 4dlu, left:pref, 4dlu, left:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.appendSeparator(Localization.lang("General"));
        builder.nextLine();
        builder.append(inspectionWarnDupli, 13);
        builder.nextLine();
        builder.append(ctrlClick, 13);
        builder.nextLine();
        builder.append(confirmDelete, 13);
        builder.nextLine();
        builder.append(keyDuplicateWarningDialog, 13);
        builder.nextLine();
        builder.append(keyEmptyWarningDialog, 13);
        builder.nextLine();
        builder.append(enforceLegalKeys, 13);
        builder.nextLine();
        builder.append(memoryStick, 13);

        // Create a new panel with its own FormLayout for the last items:
        builder.append(useOwner, 3);
        builder.append(defOwnerField);
        builder.append(overwriteOwner);
        builder.append(new JPanel(), 3);

        JButton help = new HelpAction(HelpFiles.ownerHelp).getHelpButton();
        builder.append(help);
        builder.nextLine();

        builder.append(useTimeStamp, 3);
        builder.append(timeStampFormat);
        builder.append(overwriteTimeStamp);
        builder.append(Localization.lang("Field name") + ':');
        builder.append(timeStampField);

        help = new HelpAction(HelpFiles.timeStampHelp).getHelpButton();
        builder.append(help);
        builder.nextLine();

        builder.append(new JPanel());
        builder.append(updateTimeStamp, 2);
        builder.nextLine();

        builder.append(markImportedEntries, 13);
        builder.nextLine();
        builder.append(unmarkAllEntriesBeforeImporting, 13);
        builder.nextLine();
        JLabel lab;
        lab = new JLabel(Localization.lang("Language") + ':');
        builder.append(lab, 3);
        builder.append(language);
        builder.nextLine();
        lab = new JLabel(Localization.lang("Default encoding") + ':');
        builder.append(lab, 3);
        builder.append(encodings);

        builder.nextLine();
        builder.appendSeparator(Localization.lang("Default bibliography mode"));
        builder.append(new JPanel());
        builder.append(biblatexMode);

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);

    }

    @Override
    public void setValues() {
        allowEditing.setSelected(prefs.getBoolean(JabRefPreferences.ALLOW_TABLE_EDITING));
        defSort.setSelected(prefs.getBoolean(JabRefPreferences.DEFAULT_AUTO_SORT));
        ctrlClick.setSelected(prefs.getBoolean(JabRefPreferences.CTRL_CLICK));
        useOwner.setSelected(prefs.getBoolean(JabRefPreferences.USE_OWNER));
        overwriteOwner.setSelected(prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER));
        useTimeStamp.setSelected(prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP));
        overwriteTimeStamp.setSelected(prefs.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP));
        updateTimeStamp.setSelected(prefs.getBoolean(JabRefPreferences.UPDATE_TIMESTAMP));
        updateTimeStamp.setEnabled(useTimeStamp.isSelected());
        keyDuplicateWarningDialog.setSelected(prefs.getBoolean(JabRefPreferences.DIALOG_WARNING_FOR_DUPLICATE_KEY));
        keyEmptyWarningDialog.setSelected(prefs.getBoolean(JabRefPreferences.DIALOG_WARNING_FOR_EMPTY_KEY));
        enforceLegalKeys.setSelected(prefs.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));
        memoryStick.setSelected(prefs.getBoolean(JabRefPreferences.MEMORY_STICK_MODE));
        confirmDelete.setSelected(prefs.getBoolean(JabRefPreferences.CONFIRM_DELETE));
        defOwnerField.setText(prefs.get(JabRefPreferences.DEFAULT_OWNER));
        timeStampFormat.setText(prefs.get(JabRefPreferences.TIME_STAMP_FORMAT));
        timeStampField.setText(prefs.get(JabRefPreferences.TIME_STAMP_FIELD));
        inspectionWarnDupli.setSelected(prefs.getBoolean(JabRefPreferences.WARN_ABOUT_DUPLICATES_IN_INSPECTION));
        markImportedEntries.setSelected(prefs.getBoolean(JabRefPreferences.MARK_IMPORTED_ENTRIES));
        unmarkAllEntriesBeforeImporting.setSelected(prefs.getBoolean(JabRefPreferences.UNMARK_ALL_ENTRIES_BEFORE_IMPORTING));
        if(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE)) {
            biblatexMode.setSelectedItem(BibDatabaseMode.BIBLATEX);
        } else {
            biblatexMode.setSelectedItem(BibDatabaseMode.BIBTEX);
        }

        Charset enc = Globals.prefs.getDefaultEncoding();
        encodings.setSelectedItem(enc);

        String oldLan = prefs.get(JabRefPreferences.LANGUAGE);

        // Language choice
        int ilk = 0;
        for (String lan : LANGUAGES.values()) {
            if (lan.equals(oldLan)) {
                language.setSelectedIndex(ilk);
            }
            ilk++;
        }

    }

    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.USE_OWNER, useOwner.isSelected());
        prefs.putBoolean(JabRefPreferences.OVERWRITE_OWNER, overwriteOwner.isSelected());
        prefs.putBoolean(JabRefPreferences.USE_TIME_STAMP, useTimeStamp.isSelected());
        prefs.putBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP, overwriteTimeStamp.isSelected());
        prefs.putBoolean(JabRefPreferences.UPDATE_TIMESTAMP, updateTimeStamp.isSelected());
        prefs.putBoolean(JabRefPreferences.DIALOG_WARNING_FOR_DUPLICATE_KEY, keyDuplicateWarningDialog.isSelected());
        prefs.putBoolean(JabRefPreferences.DIALOG_WARNING_FOR_EMPTY_KEY, keyEmptyWarningDialog.isSelected());
        prefs.putBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY, enforceLegalKeys.isSelected());
        if (prefs.getBoolean(JabRefPreferences.MEMORY_STICK_MODE) && !memoryStick.isSelected()) {
            JOptionPane.showMessageDialog(null, Localization.lang("To disable the memory stick mode"
                            + " rename or remove the jabref.xml file in the same folder as JabRef."),
                    Localization.lang("Memory Stick Mode"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
        prefs.putBoolean(JabRefPreferences.MEMORY_STICK_MODE, memoryStick.isSelected());
        prefs.putBoolean(JabRefPreferences.CONFIRM_DELETE, confirmDelete.isSelected());
        prefs.putBoolean(JabRefPreferences.ALLOW_TABLE_EDITING, allowEditing.isSelected());
        prefs.putBoolean(JabRefPreferences.CTRL_CLICK, ctrlClick.isSelected());
        prefs.putBoolean(JabRefPreferences.WARN_ABOUT_DUPLICATES_IN_INSPECTION, inspectionWarnDupli.isSelected());
        String owner = defOwnerField.getText().trim();
        prefs.put(JabRefPreferences.DEFAULT_OWNER, owner);
        prefs.WRAPPED_USERNAME = '[' + owner + ']';
        prefs.put(JabRefPreferences.TIME_STAMP_FORMAT, timeStampFormat.getText().trim());
        prefs.put(JabRefPreferences.TIME_STAMP_FIELD, timeStampField.getText().trim());
        prefs.setDefaultEncoding((Charset) encodings.getSelectedItem());
        prefs.putBoolean(JabRefPreferences.MARK_IMPORTED_ENTRIES, markImportedEntries.isSelected());
        prefs.putBoolean(JabRefPreferences.UNMARK_ALL_ENTRIES_BEFORE_IMPORTING, unmarkAllEntriesBeforeImporting.isSelected());
        prefs.putBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE, biblatexMode.getSelectedItem() == BibDatabaseMode.BIBLATEX);

        if (!LANGUAGES.get(language.getSelectedItem()).equals(prefs.get(JabRefPreferences.LANGUAGE))) {
            prefs.put(JabRefPreferences.LANGUAGE, LANGUAGES.get(language.getSelectedItem()));
            Localization.setLanguage(LANGUAGES.get(language.getSelectedItem()));
            // Update any defaults that might be language dependent:
            Globals.prefs.setLanguageDependentDefaultValues();
            // Warn about restart needed:
            JOptionPane.showMessageDialog(null,
                    Localization.lang("You have changed the language setting.")
                            .concat(" ")
                            .concat(Localization.lang("You must restart JabRef for this to come into effect.")),
                    Localization.lang("Changed language settings"),
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public boolean validateSettings() {
        try {
            // Test if date format is legal:
            new SimpleDateFormat(timeStampFormat.getText());

        } catch (IllegalArgumentException ex2) {
            JOptionPane.showMessageDialog
                    (null, Localization.lang("The chosen date format for new entries is not valid"),
                            Localization.lang("Invalid date format"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("General");
    }
}
