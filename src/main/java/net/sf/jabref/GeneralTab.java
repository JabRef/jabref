/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.help.HelpAction;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class GeneralTab extends JPanel implements PrefsTab {

    private final JCheckBox
            defSort;
    private final JCheckBox ctrlClick;
    private final JCheckBox useOwner;
    private final JCheckBox overwriteOwner;
    private final JCheckBox keyDuplicateWarningDialog;
    private final JCheckBox keyEmptyWarningDialog;
    private final JCheckBox enforceLegalKeys;
    private final JCheckBox confirmDelete;
    private final JCheckBox allowEditing;
    private final JCheckBox memoryStick;
    private final JCheckBox useImportInspector;
    private final JCheckBox useImportInspectorForSingle;
    private final JCheckBox inspectionWarnDupli;
    private final JCheckBox useTimeStamp;
    private final JCheckBox updateTimeStamp;
    private final JCheckBox overwriteTimeStamp;
    private final JCheckBox markImportedEntries;
    private final JCheckBox unmarkAllEntriesBeforeImporting;

    private final JTextField defOwnerField;
    private final JTextField timeStampFormat;
    private final JTextField timeStampField;
    private final JabRefPreferences _prefs;
    private final JComboBox language = new JComboBox(GUIGlobals.LANGUAGES.keySet().toArray(new String[GUIGlobals.LANGUAGES.keySet().size()]));
    private final JComboBox encodings = new JComboBox(Globals.ENCODINGS);


    public GeneralTab(JabRefFrame frame, JabRefPreferences prefs) {
        _prefs = prefs;
        setLayout(new BorderLayout());

        allowEditing = new JCheckBox(Globals.lang("Allow editing in table cells"));

        memoryStick = new JCheckBox(Globals.lang("Load and Save preferences from/to jabref.xml on start-up (memory stick mode)"));
        defSort = new JCheckBox(Globals.lang("Sort Automatically"));
        ctrlClick = new JCheckBox(Globals.lang("Open right-click menu with Ctrl+left button"));
        useOwner = new JCheckBox(Globals.lang("Mark new entries with owner name") + ':');
        useTimeStamp = new JCheckBox(Globals.lang("Mark new entries with addition date") + ". "
                + Globals.lang("Date format") + ':');
        useTimeStamp.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent arg0) {
                updateTimeStamp.setEnabled(useTimeStamp.isSelected());
            }
        });
        updateTimeStamp = new JCheckBox(Globals.lang("Update timestamp on modification"));
        overwriteOwner = new JCheckBox(Globals.lang("Overwrite"));
        overwriteTimeStamp = new JCheckBox(Globals.lang("Overwrite"));
        overwriteOwner.setToolTipText(Globals.lang("If a pasted or imported entry already has "
                + "the field set, overwrite."));
        overwriteTimeStamp.setToolTipText(Globals.lang("If a pasted or imported entry already has "
                + "the field set, overwrite."));
        keyDuplicateWarningDialog = new JCheckBox(Globals.lang("Show warning dialog when a duplicate BibTeX key is entered"));
        keyEmptyWarningDialog = new JCheckBox(Globals.lang("Show warning dialog when an empty BibTeX key is entered")); // JZTODO lyrics
        enforceLegalKeys = new JCheckBox(Globals.lang("Enforce legal characters in BibTeX keys"));
        confirmDelete = new JCheckBox(Globals.lang("Show confirmation dialog when deleting entries"));

        useImportInspector = new JCheckBox(Globals.lang("Display imported entries in an inspection window before they are added."));
        useImportInspectorForSingle = new JCheckBox(Globals.lang("Use inspection window also when a single entry is imported."));
        markImportedEntries = new JCheckBox(Globals.lang("Mark entries imported into an existing database"));
        unmarkAllEntriesBeforeImporting = new JCheckBox(Globals.lang("Unmark all entries before importing new entries into an existing database"));
        defOwnerField = new JTextField();
        timeStampFormat = new JTextField();
        timeStampField = new JTextField();
        HelpAction ownerHelp = new HelpAction(frame.helpDiag, GUIGlobals.ownerHelp,
                "Help", GUIGlobals.getIconUrl("helpSmall"));
        HelpAction timeStampHelp = new HelpAction(frame.helpDiag, GUIGlobals.timeStampHelp, "Help",
                GUIGlobals.getIconUrl("helpSmall"));
        inspectionWarnDupli = new JCheckBox(Globals.lang("Warn about unresolved duplicates when closing inspection window"));

        Insets marg = new Insets(0, 12, 3, 0);
        useImportInspectorForSingle.setMargin(marg);
        inspectionWarnDupli.setMargin(marg);

        // We need a listener on useImportInspector to enable and disable the
        // import inspector related choices;
        useImportInspector.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent event) {
                useImportInspectorForSingle.setEnabled(useImportInspector.isSelected());
                inspectionWarnDupli.setEnabled(useImportInspector.isSelected());
            }
        });

        FormLayout layout = new FormLayout
                ("8dlu, 1dlu, left:170dlu, 4dlu, fill:pref, 4dlu, fill:pref, 4dlu, left:pref, 4dlu, left:pref, 4dlu, left:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.appendSeparator(Globals.lang("General"));
        builder.nextLine();
        builder.append(useImportInspector, 13);
        builder.nextLine();
        builder.append(new JPanel());
        builder.append(useImportInspectorForSingle, 11);
        builder.nextLine();
        builder.append(new JPanel());
        builder.append(inspectionWarnDupli, 11);
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

        JButton hlp = new JButton(ownerHelp);
        hlp.setText(null);
        hlp.setPreferredSize(new Dimension(24, 24));
        builder.append(hlp);
        builder.nextLine();

        builder.append(useTimeStamp, 3);
        builder.append(timeStampFormat);
        builder.append(overwriteTimeStamp);
        builder.append(Globals.lang("Field name") + ':');
        builder.append(timeStampField);

        hlp = new JButton(timeStampHelp);
        hlp.setText(null);
        hlp.setPreferredSize(new Dimension(24, 24));
        builder.append(hlp);
        builder.nextLine();

        builder.append(new JPanel());
        builder.append(updateTimeStamp, 2);
        builder.nextLine();

        builder.append(markImportedEntries, 13);
        builder.nextLine();
        builder.append(unmarkAllEntriesBeforeImporting, 13);
        builder.nextLine();
        JLabel lab;
        lab = new JLabel(Globals.lang("Language") + ':');
        builder.append(lab, 3);
        builder.append(language);
        builder.nextLine();
        lab = new JLabel(Globals.lang("Default encoding") + ':');
        builder.append(lab, 3);
        builder.append(encodings);

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);

    }

    @Override
    public void setValues() {
        allowEditing.setSelected(_prefs.getBoolean(JabRefPreferences.ALLOW_TABLE_EDITING));
        defSort.setSelected(_prefs.getBoolean(JabRefPreferences.DEFAULT_AUTO_SORT));
        ctrlClick.setSelected(_prefs.getBoolean(JabRefPreferences.CTRL_CLICK));
        useOwner.setSelected(_prefs.getBoolean(JabRefPreferences.USE_OWNER));
        overwriteOwner.setSelected(_prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER));
        useTimeStamp.setSelected(_prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP));
        overwriteTimeStamp.setSelected(_prefs.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP));
        updateTimeStamp.setSelected(_prefs.getBoolean(JabRefPreferences.UPDATE_TIMESTAMP));
        updateTimeStamp.setEnabled(useTimeStamp.isSelected());
        keyDuplicateWarningDialog.setSelected(_prefs.getBoolean(JabRefPreferences.DIALOG_WARNING_FOR_DUPLICATE_KEY));
        keyEmptyWarningDialog.setSelected(_prefs.getBoolean(JabRefPreferences.DIALOG_WARNING_FOR_EMPTY_KEY));
        enforceLegalKeys.setSelected(_prefs.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));
        memoryStick.setSelected(_prefs.getBoolean(JabRefPreferences.MEMORY_STICK_MODE));
        confirmDelete.setSelected(_prefs.getBoolean(JabRefPreferences.CONFIRM_DELETE));
        defOwnerField.setText(_prefs.get(JabRefPreferences.DEFAULT_OWNER));
        timeStampFormat.setText(_prefs.get(JabRefPreferences.TIME_STAMP_FORMAT));
        timeStampField.setText(_prefs.get(JabRefPreferences.TIME_STAMP_FIELD));
        useImportInspector.setSelected(_prefs.getBoolean(JabRefPreferences.USE_IMPORT_INSPECTION_DIALOG));
        useImportInspectorForSingle.setSelected(_prefs.getBoolean(JabRefPreferences.USE_IMPORT_INSPECTION_DIALOG_FOR_SINGLE));
        inspectionWarnDupli.setSelected(_prefs.getBoolean(JabRefPreferences.WARN_ABOUT_DUPLICATES_IN_INSPECTION));
        useImportInspectorForSingle.setEnabled(useImportInspector.isSelected());
        inspectionWarnDupli.setEnabled(useImportInspector.isSelected());
        markImportedEntries.setSelected(_prefs.getBoolean(JabRefPreferences.MARK_IMPORTED_ENTRIES));
        unmarkAllEntriesBeforeImporting.setSelected(_prefs.getBoolean(JabRefPreferences.UNMARK_ALL_ENTRIES_BEFORE_IMPORTING));

        String enc = _prefs.get(JabRefPreferences.DEFAULT_ENCODING);
        for (int i = 0; i < Globals.ENCODINGS.length; i++) {
            if (Globals.ENCODINGS[i].equalsIgnoreCase(enc)) {
                encodings.setSelectedIndex(i);
                break;
            }
        }
        String oldLan = _prefs.get(JabRefPreferences.LANGUAGE);

        // Language choice
        int ilk = 0;
        for (String lan : GUIGlobals.LANGUAGES.values()) {
            if (lan.equals(oldLan)) {
                language.setSelectedIndex(ilk);
            }
            ilk++;
        }

    }

    @Override
    public void storeSettings() {
        _prefs.putBoolean(JabRefPreferences.USE_OWNER, useOwner.isSelected());
        _prefs.putBoolean(JabRefPreferences.OVERWRITE_OWNER, overwriteOwner.isSelected());
        _prefs.putBoolean(JabRefPreferences.USE_TIME_STAMP, useTimeStamp.isSelected());
        _prefs.putBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP, overwriteTimeStamp.isSelected());
        _prefs.putBoolean(JabRefPreferences.UPDATE_TIMESTAMP, updateTimeStamp.isSelected());
        _prefs.putBoolean(JabRefPreferences.DIALOG_WARNING_FOR_DUPLICATE_KEY, keyDuplicateWarningDialog.isSelected());
        _prefs.putBoolean(JabRefPreferences.DIALOG_WARNING_FOR_EMPTY_KEY, keyEmptyWarningDialog.isSelected());
        _prefs.putBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY, enforceLegalKeys.isSelected());
        if (_prefs.getBoolean(JabRefPreferences.MEMORY_STICK_MODE) && !memoryStick.isSelected()) {
            JOptionPane.showMessageDialog(null, Globals.lang("To disable the memory stick mode"
                    + " rename or remove the jabref.xml file in the same folder as JabRef."),
                    Globals.lang("Memory Stick Mode"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
        _prefs.putBoolean(JabRefPreferences.MEMORY_STICK_MODE, memoryStick.isSelected());
        _prefs.putBoolean(JabRefPreferences.CONFIRM_DELETE, confirmDelete.isSelected());
        _prefs.putBoolean(JabRefPreferences.ALLOW_TABLE_EDITING, allowEditing.isSelected());
        _prefs.putBoolean(JabRefPreferences.CTRL_CLICK, ctrlClick.isSelected());
        //_prefs.putBoolean("preserveFieldFormatting", preserveFormatting.isSelected());
        _prefs.putBoolean(JabRefPreferences.USE_IMPORT_INSPECTION_DIALOG, useImportInspector.isSelected());
        _prefs.putBoolean(JabRefPreferences.USE_IMPORT_INSPECTION_DIALOG_FOR_SINGLE, useImportInspectorForSingle.isSelected());
        _prefs.putBoolean(JabRefPreferences.WARN_ABOUT_DUPLICATES_IN_INSPECTION, inspectionWarnDupli.isSelected());
        //_prefs.putBoolean("defaultAutoSort", defSorrrt.isSelected());
        String owner = defOwnerField.getText().trim();
        _prefs.put(JabRefPreferences.DEFAULT_OWNER, owner);
        _prefs.WRAPPED_USERNAME = '[' + owner + ']';
        _prefs.put(JabRefPreferences.TIME_STAMP_FORMAT, timeStampFormat.getText().trim());
        _prefs.put(JabRefPreferences.TIME_STAMP_FIELD, timeStampField.getText().trim());
        _prefs.put(JabRefPreferences.DEFAULT_ENCODING, (String) encodings.getSelectedItem());
        _prefs.putBoolean(JabRefPreferences.MARK_IMPORTED_ENTRIES, markImportedEntries.isSelected());
        _prefs.putBoolean(JabRefPreferences.UNMARK_ALL_ENTRIES_BEFORE_IMPORTING, unmarkAllEntriesBeforeImporting.isSelected());

        if (!GUIGlobals.LANGUAGES.get(language.getSelectedItem()).equals(_prefs.get(JabRefPreferences.LANGUAGE))) {
            _prefs.put(JabRefPreferences.LANGUAGE, GUIGlobals.LANGUAGES.get(language.getSelectedItem()));
            Globals.setLanguage(GUIGlobals.LANGUAGES.get(language.getSelectedItem()), "");
            // Update any defaults that might be language dependent:
            Globals.prefs.setLanguageDependentDefaultValues();
            // Warn about restart needed:
            JOptionPane.showMessageDialog(null,
                    Globals.lang("You have changed the language setting.")
                            .concat(" ")
                            .concat(Globals.lang("You must restart JabRef for this to come into effect.")),
                    Globals.lang("Changed language settings"),
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public boolean readyToClose() {
        try {
            // Test if date format is legal:
            new SimpleDateFormat(timeStampFormat.getText());

        } catch (IllegalArgumentException ex2) {
            JOptionPane.showMessageDialog
                    (null, Globals.lang("The chosen date format for new entries is not valid"),
                            Globals.lang("Invalid date format"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    @Override
    public String getTabName() {
        return Globals.lang("General");
    }
}
