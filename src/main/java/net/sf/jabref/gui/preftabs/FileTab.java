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
package net.sf.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.help.HelpAction;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Preferences tab for file options. These options were moved out from GeneralTab to
 * resolve the space issue.
 */
class FileTab extends JPanel implements PrefsTab {

    private final JabRefPreferences prefs;
    private final JabRefFrame frame;

    private final JCheckBox backup;
    private final JCheckBox openLast;
    private final JCheckBox autoDoubleBraces;
    private final JCheckBox autoSave;
    private final JCheckBox promptBeforeUsingAutoSave;
    private final JComboBox<String> valueDelimiter;
    private final JComboBox<String> newlineSeparator;
    private final JRadioButton resolveStringsStandard;
    private final JRadioButton resolveStringsAll;
    private final JTextField bracesAroundCapitalsFields;
    private final JTextField nonWrappableFields;
    private final JTextField doNotResolveStringsFor;
    private final JSpinner autoSaveInterval;
    private boolean origAutoSaveSetting;


    public FileTab(JabRefFrame frame, JabRefPreferences prefs) {
        this.prefs = prefs;
        this.frame = frame;

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

        bracesAroundCapitalsFields = new JTextField(25);
        nonWrappableFields = new JTextField(25);
        doNotResolveStringsFor = new JTextField(30);
        autoDoubleBraces = new JCheckBox(Localization.lang("Remove double braces around BibTeX fields when loading."));

        autoSave.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                autoSaveInterval.setEnabled(autoSave.isSelected());
                promptBeforeUsingAutoSave.setEnabled(autoSave.isSelected());
            }
        });

        FormLayout layout = new FormLayout("left:pref, 4dlu, fill:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.appendSeparator(Localization.lang("General"));
        builder.nextLine();
        builder.append(openLast, 3);
        builder.nextLine();
        builder.append(backup, 3);
        builder.nextLine();
        builder.append(autoDoubleBraces, 3);
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

        builder.appendSeparator(Localization.lang("Autosave"));
        builder.append(autoSave, 1);
        JButton help = new HelpAction(frame.helpDiag, GUIGlobals.autosaveHelp).getIconButton();
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

        autoDoubleBraces.setSelected(prefs.getBoolean(JabRefPreferences.AUTO_DOUBLE_BRACES));
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
        }
        prefs.put(JabRefPreferences.NEWLINE, newline);
        // we also have to change Globals variable as globals is not a getter, but a constant
        Globals.NEWLINE = newline;

        prefs.putBoolean(JabRefPreferences.BACKUP, backup.isSelected());
        prefs.putBoolean(JabRefPreferences.OPEN_LAST_EDITED, openLast.isSelected());
        prefs.putBoolean(JabRefPreferences.AUTO_DOUBLE_BRACES, autoDoubleBraces.isSelected());
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
