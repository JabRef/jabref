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

import java.awt.BorderLayout;
import java.awt.Insets;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.autocompleter.AutoCompleterFactory;
import net.sf.jabref.logic.l10n.Localization;
import org.xnap.commons.gui.shortcut.EmacsKeyBindings;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

class EntryEditorPrefsTab extends JPanel implements PrefsTab {

    private final JCheckBox autoOpenForm;
    private final JCheckBox showSource;
    private final JCheckBox defSource;
    private final JCheckBox emacsMode;
    private final JCheckBox emacsRebindCtrlA;
    private final JCheckBox emacsRebindCtrlF;
    private final JCheckBox disableOnMultiple;
    private final JCheckBox autoComplete;
    private final JRadioButton autoCompBoth;
    private final JRadioButton autoCompFF;
    private final JRadioButton autoCompLF;
    private final JRadioButton firstNameModeFull;
    private final JRadioButton firstNameModeAbbr;
    private final JRadioButton firstNameModeBoth;
    private boolean oldAutoCompFF;
    private boolean oldAutoCompLF;
    private boolean oldAutoCompFModeAbbr;
    private boolean oldAutoCompFModeFull;
    private final JSpinner shortestToComplete;

    private final JTextField autoCompFields;
    private final JabRefPreferences prefs;
    private final JabRefFrame frame;


    private void setAutoCompleteElementsEnabled(boolean enabled) {
        autoCompFields.setEnabled(enabled);
        autoCompLF.setEnabled(enabled);
        autoCompFF.setEnabled(enabled);
        autoCompBoth.setEnabled(enabled);
        firstNameModeAbbr.setEnabled(enabled);
        firstNameModeFull.setEnabled(enabled);
        firstNameModeBoth.setEnabled(enabled);
        shortestToComplete.setEnabled(enabled);
    }

    public EntryEditorPrefsTab(JabRefFrame frame, JabRefPreferences prefs) {
        this.prefs = prefs;
        this.frame = frame;
        setLayout(new BorderLayout());

        autoOpenForm = new JCheckBox(Localization.lang("Open editor when a new entry is created"));
        defSource = new JCheckBox(Localization.lang("Show BibTeX source by default"));
        showSource = new JCheckBox(Localization.lang("Show BibTeX source panel"));
        emacsMode = new JCheckBox(Localization.lang("Use Emacs key bindings"));
        emacsRebindCtrlA = new JCheckBox(Localization.lang("Rebind C-a, too"));
        emacsRebindCtrlF = new JCheckBox(Localization.lang("Rebind C-f, too"));
        disableOnMultiple = new JCheckBox(Localization.lang("Disable entry editor when multiple entries are selected"));
        autoComplete = new JCheckBox(Localization.lang("Enable word/name autocompletion"));

        shortestToComplete = new JSpinner(new SpinnerNumberModel(prefs.getInt(JabRefPreferences.SHORTEST_TO_COMPLETE), 1, 5, 1));

        // allowed name formats
        autoCompFF = new JRadioButton(Localization.lang("Autocomplete names in 'Firstname Lastname' format only"));
        autoCompLF = new JRadioButton(Localization.lang("Autocomplete names in 'Lastname, Firstname' format only"));
        autoCompBoth = new JRadioButton(Localization.lang("Autocomplete names in both formats"));
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(autoCompLF);
        buttonGroup.add(autoCompFF);
        buttonGroup.add(autoCompBoth);

        // treatment of first name
        firstNameModeFull = new JRadioButton(Localization.lang("Use full firstname whenever possible"));
        firstNameModeAbbr = new JRadioButton(Localization.lang("Use abbreviated firstname whenever possible"));
        firstNameModeBoth = new JRadioButton(Localization.lang("Use abbreviated and full firstname"));
        ButtonGroup firstNameModeButtonGroup = new ButtonGroup();
        firstNameModeButtonGroup.add(firstNameModeFull);
        firstNameModeButtonGroup.add(firstNameModeAbbr);
        firstNameModeButtonGroup.add(firstNameModeBoth);

        Insets marg = new Insets(0, 20, 3, 0);
        defSource.setMargin(marg);
        // We need a listener on showSource to enable and disable the source panel-related choices:
        showSource.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent event) {
                defSource.setEnabled(showSource.isSelected());
            }
        }
                );

        emacsRebindCtrlA.setMargin(marg);
        // We need a listener on showSource to enable and disable the source panel-related choices:
        emacsMode.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent event) {
                emacsRebindCtrlA.setEnabled(emacsMode.isSelected());
            }
        }
                );

        emacsRebindCtrlF.setMargin(marg);
        // We need a listener on showSource to enable and disable the source panel-related choices:
        emacsMode.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                emacsRebindCtrlF.setEnabled(emacsMode.isSelected());
            }
        }
        );

        
        autoCompFields = new JTextField(40);
        // We need a listener on autoComplete to enable and disable the
        // autoCompFields text field:
        autoComplete.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent event) {
                setAutoCompleteElementsEnabled(autoComplete.isSelected());
            }
        }
                );

        FormLayout layout = new FormLayout
                (// columns
                 "8dlu, left:pref, 8dlu, fill:150dlu, 4dlu, fill:pref", // 4dlu, left:pref, 4dlu",
                 // rows  1 to 10
                 "pref, 6dlu, pref, 6dlu, pref, 6dlu, pref, 6dlu, pref, 6dlu, " +
                 // rows 11 to 20
                 "pref, 6dlu, pref, 6dlu, pref, 6dlu, pref, 6dlu, pref, 6dlu, " +
                 // rows 21 to 31
                 "pref, 6dlu, pref, pref, pref, pref, 6dlu, pref, pref, pref, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.addSeparator(Localization.lang("Editor options"), cc.xyw(1, 1, 5));
        builder.add(autoOpenForm, cc.xy(2, 3));
        builder.add(disableOnMultiple, cc.xy(2, 5));
        builder.add(showSource, cc.xy(2, 7));
        builder.add(defSource, cc.xy(2, 9));
        builder.add(emacsMode, cc.xy(2, 11));
        builder.add(emacsRebindCtrlA, cc.xy(2, 13));
        builder.add(emacsRebindCtrlF, cc.xy(2, 15));
        
        builder.addSeparator(Localization.lang("Autocompletion options"), cc.xyw(1, 17, 5));
        builder.add(autoComplete, cc.xy(2, 19));
        
        DefaultFormBuilder builder3 = new DefaultFormBuilder(new FormLayout("left:pref, 4dlu, fill:150dlu",""));
        JLabel label = new JLabel(Localization.lang("Use autocompletion for the following fields")+":");

        builder3.append(label);
        builder3.append(autoCompFields);
        JLabel label2 = new JLabel(Localization.lang("Autocomplete after following number of characters") + ":");
        builder3.append(label2);
        builder3.append(shortestToComplete);
        builder.add(builder3.getPanel(), cc.xyw(2, 21, 3));

        builder.addSeparator(Localization.lang("Name format used for autocompletion"), cc.xyw(2, 23, 4));
        builder.add(autoCompFF, cc.xy(2,24));
        builder.add(autoCompLF, cc.xy(2,25));
        builder.add(autoCompBoth, cc.xy(2,26));
        
        builder.addSeparator(Localization.lang("Treatment of first names"), cc.xyw(2, 28, 4));
        builder.add(firstNameModeAbbr, cc.xy(2,29));
        builder.add(firstNameModeFull, cc.xy(2,30));
        builder.add(firstNameModeBoth, cc.xy(2,31));
        
        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        autoOpenForm.setSelected(prefs.getBoolean(JabRefPreferences.AUTO_OPEN_FORM));
        defSource.setSelected(prefs.getBoolean(JabRefPreferences.DEFAULT_SHOW_SOURCE));
        showSource.setSelected(prefs.getBoolean(JabRefPreferences.SHOW_SOURCE));
        emacsMode.setSelected(prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS));
        emacsRebindCtrlA.setSelected(prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CA));
        emacsRebindCtrlF.setSelected(prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CF));
        disableOnMultiple.setSelected(prefs.getBoolean(JabRefPreferences.DISABLE_ON_MULTIPLE_SELECTION));
        autoComplete.setSelected(prefs.getBoolean(JabRefPreferences.AUTO_COMPLETE));
        autoCompFields.setText(prefs.get(JabRefPreferences.AUTO_COMPLETE_FIELDS));
        shortestToComplete.setValue(prefs.getInt(JabRefPreferences.SHORTEST_TO_COMPLETE));

        if (prefs.getBoolean(JabRefPreferences.AUTO_COMP_FIRST_LAST)) {
            autoCompFF.setSelected(true);
        } else if (prefs.getBoolean(JabRefPreferences.AUTO_COMP_LAST_FIRST)) {
            autoCompLF.setSelected(true);
        } else {
            autoCompBoth.setSelected(true);
        }
        oldAutoCompFF = autoCompFF.isSelected();
        oldAutoCompLF = autoCompLF.isSelected();

        if (prefs.get(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE).equals(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_ABBR)) {
            firstNameModeAbbr.setSelected(true);
        } else if (prefs.get(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE).equals(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_FULL)) {
            firstNameModeFull.setSelected(true);
        } else {
            firstNameModeBoth.setSelected(true);
        }
        // one field less than the option is enough. If one filed changes, another one also changes.
        oldAutoCompFModeAbbr = firstNameModeAbbr.isSelected();
        oldAutoCompFModeFull = firstNameModeFull.isSelected();

        // This choice only makes sense when the source panel is visible:
        defSource.setEnabled(showSource.isSelected());
        // similar for emacs CTRL-a and emacs mode
        emacsRebindCtrlA.setEnabled(emacsMode.isSelected());
        // Autocomplete fields is only enabled when autocompletion is selected
        setAutoCompleteElementsEnabled(autoComplete.isSelected());
    }

    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.AUTO_OPEN_FORM, autoOpenForm.isSelected());
        prefs.putBoolean(JabRefPreferences.DEFAULT_SHOW_SOURCE, defSource.isSelected());
        boolean emacsModeChanged = prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS) != emacsMode.isSelected();
        boolean emacsRebindCtrlAChanged = prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CA) != emacsRebindCtrlA.isSelected();
        boolean emacsRebindCtrlFChanged = prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CF) != emacsRebindCtrlF.isSelected();
        if (emacsModeChanged || emacsRebindCtrlAChanged || emacsRebindCtrlFChanged) {
            prefs.putBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS, emacsMode.isSelected());
            prefs.putBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CA, emacsRebindCtrlA.isSelected());
            prefs.putBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CF, emacsRebindCtrlF.isSelected());
            // immediately apply the change
            if (emacsModeChanged) {
                if (emacsMode.isSelected()) {
                    EmacsKeyBindings.load();
                } else {
                    EmacsKeyBindings.unload();
                }
            } else {
                // only rebinding of CTRL+a or CTRL+f changed
                assert emacsMode.isSelected();
                // we simply reload the emacs mode to activate the CTRL+a/CTRL+f change
                EmacsKeyBindings.unload();
                EmacsKeyBindings.load();
            }
        }
        prefs.putBoolean(JabRefPreferences.DISABLE_ON_MULTIPLE_SELECTION, disableOnMultiple.isSelected());
        // We want to know if the following settings have been modified:
        boolean oldAutoComplete = prefs.getBoolean(JabRefPreferences.AUTO_COMPLETE);
        boolean oldShowSource = prefs.getBoolean(JabRefPreferences.SHOW_SOURCE);
        String oldAutoCompFields = prefs.get(JabRefPreferences.AUTO_COMPLETE_FIELDS);
        prefs.putInt(JabRefPreferences.SHORTEST_TO_COMPLETE, (Integer) shortestToComplete.getValue());
        prefs.putBoolean(JabRefPreferences.AUTO_COMPLETE, autoComplete.isSelected());
        prefs.put(JabRefPreferences.AUTO_COMPLETE_FIELDS, autoCompFields.getText());
        prefs.putBoolean(JabRefPreferences.SHOW_SOURCE, showSource.isSelected());
        if (autoCompBoth.isSelected()) {
            prefs.putBoolean(JabRefPreferences.AUTO_COMP_FIRST_LAST, false);
            prefs.putBoolean(JabRefPreferences.AUTO_COMP_LAST_FIRST, false);
        }
        else if (autoCompFF.isSelected()) {
            prefs.putBoolean(JabRefPreferences.AUTO_COMP_FIRST_LAST, true);
            prefs.putBoolean(JabRefPreferences.AUTO_COMP_LAST_FIRST, false);
        }
        else {
            prefs.putBoolean(JabRefPreferences.AUTO_COMP_FIRST_LAST, false);
            prefs.putBoolean(JabRefPreferences.AUTO_COMP_LAST_FIRST, true);
        }
        if (firstNameModeAbbr.isSelected()) {
            prefs.put(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE, JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_ABBR);
        } else if (firstNameModeFull.isSelected()) {
            prefs.put(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE, JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_FULL);
        } else {
            prefs.put(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE, JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_BOTH);
        }

        // We need to remove all entry editors from cache if the source panel setting
        // or the autocompletion settings have been changed:
        if (oldShowSource != showSource.isSelected() || oldAutoComplete != autoComplete.isSelected()
                || !oldAutoCompFields.equals(autoCompFields.getText()) ||
                oldAutoCompFF != autoCompFF.isSelected() || oldAutoCompLF != autoCompLF.isSelected() ||
                oldAutoCompFModeAbbr != firstNameModeAbbr.isSelected() ||
                oldAutoCompFModeFull != firstNameModeFull.isSelected()) {
            for (int j = 0; j < frame.getTabbedPane().getTabCount(); j++) {
                BasePanel bp = (BasePanel) frame.getTabbedPane().getComponentAt(j);
                bp.entryEditors.clear();
            }
        }
        // the autocompleter has to be updated to the new min length to complete 
        AutoCompleterFactory.SHORTEST_TO_COMPLETE = (Integer) shortestToComplete.getValue();
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry editor");
    }
}
