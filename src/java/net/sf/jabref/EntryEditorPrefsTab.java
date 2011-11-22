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
import java.awt.Insets;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class EntryEditorPrefsTab extends JPanel implements PrefsTab {

    private JCheckBox autoOpenForm, showSource,
        defSource, editSource, disableOnMultiple, autoComplete;
    private JRadioButton autoCompBoth, autoCompFF, autoCompLF;
    boolean oldAutoCompFF, oldAutoCompLF;

    private JTextField autoCompFields;
    JabRefPreferences _prefs;
    JabRefFrame _frame;

    public EntryEditorPrefsTab(JabRefFrame frame, JabRefPreferences prefs) {
        _prefs = prefs;
        _frame = frame;
        setLayout(new BorderLayout());


        autoOpenForm = new JCheckBox(Globals.lang("Open editor when a new entry is created"));
        defSource = new JCheckBox(Globals.lang("Show BibTeX source by default"));
        showSource = new JCheckBox(Globals.lang("Show BibTeX source panel"));
        editSource = new JCheckBox(Globals.lang("Enable source editing"));
        disableOnMultiple = new JCheckBox(Globals.lang("Disable entry editor when multiple entries are selected"));
        autoComplete = new JCheckBox(Globals.lang("Enable word/name autocompletion"));
        autoCompFF = new JRadioButton(Globals.lang("Autocomplete names in 'Firstname Lastname' format only"));
        autoCompLF = new JRadioButton(Globals.lang("Autocomplete names in 'Lastname, Firstname' format only"));
        autoCompBoth = new JRadioButton(Globals.lang("Autocomplete names in both formats"));
        ButtonGroup bg = new ButtonGroup();
        bg.add(autoCompLF);
        bg.add(autoCompFF);
        bg.add(autoCompBoth);
        autoCompFields = new JTextField(40);
        Insets marg = new Insets(0,12,3,0);
        editSource.setMargin(marg);
        defSource.setMargin(marg);
        // We need a listener on showSource to enable and disable the source panel-related choices:
        showSource.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                defSource.setEnabled(showSource.isSelected());
                editSource.setEnabled(showSource.isSelected());
            }
        }
        );
        // We need a listener on autoComplete to enable and disable the
        // autoCompFields text field:
        autoComplete.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                autoCompFields.setEnabled(autoComplete.isSelected());
            }
        }
        );

        FormLayout layout = new FormLayout
                ("8dlu, left:pref, 8dlu, fill:150dlu, 4dlu, fill:pref", // 4dlu, left:pref, 4dlu",
                        "pref, 6dlu, pref, 6dlu, pref, 6dlu, pref, 6dlu, pref, 6dlu, "
                    +"pref, 6dlu, pref, 6dlu, pref, 6dlu, pref, 6dlu, pref, 6dlu, pref, 6dlu, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.addSeparator(Globals.lang("Editor options"), cc.xyw(1,1, 5));
        builder.add(autoOpenForm, cc.xy(2, 3));
        builder.add(disableOnMultiple, cc.xy(2, 5));
        builder.add(showSource, cc.xy(2, 7));
        builder.add(defSource, cc.xy(2, 9));
        builder.add(autoComplete, cc.xy(2, 11));
        JLabel label = new JLabel(Globals.lang("Use autocompletion for the following fields")+":");
        DefaultFormBuilder builder3 = new DefaultFormBuilder
                (new FormLayout("left:pref, 4dlu, fill:150dlu",""));
        builder3.append(label);
        builder3.append(autoCompFields);
        builder.add(builder3.getPanel(), cc.xyw(2, 13, 3));
        builder.add(autoCompFF, cc.xy(2,15));
        builder.add(autoCompLF, cc.xy(2,17));
        builder.add(autoCompBoth, cc.xy(2,19));
        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);

    }

    public void setValues() {
        autoOpenForm.setSelected(_prefs.getBoolean("autoOpenForm"));
        defSource.setSelected(_prefs.getBoolean("defaultShowSource"));
        showSource.setSelected(_prefs.getBoolean("showSource"));
        editSource.setSelected(_prefs.getBoolean("enableSourceEditing"));
        disableOnMultiple.setSelected(_prefs.getBoolean("disableOnMultipleSelection"));
        autoComplete.setSelected(_prefs.getBoolean("autoComplete"));
        autoCompFields.setText(_prefs.get("autoCompleteFields"));
        // Two choices only make sense when the source panel is visible:
        defSource.setEnabled(showSource.isSelected());
        editSource.setEnabled(showSource.isSelected());
        // Autocomplete fields is only enabled when autocompletion is:
        autoCompFields.setEnabled(autoComplete.isSelected());
        if (_prefs.getBoolean("autoCompFF"))
            autoCompFF.setSelected(true);
        else if (_prefs.getBoolean("autoCompLF"))
            autoCompLF.setSelected(true);
        else
            autoCompBoth.setSelected(true);
        oldAutoCompFF = autoCompFF.isSelected();
        oldAutoCompLF = autoCompLF.isSelected();
    }

    public void storeSettings() {
        _prefs.putBoolean("autoOpenForm", autoOpenForm.isSelected());
        _prefs.putBoolean("defaultShowSource", defSource.isSelected());
        _prefs.putBoolean("enableSourceEditing", editSource.isSelected());
        _prefs.putBoolean("disableOnMultipleSelection", disableOnMultiple.isSelected());
        // We want to know if the following settings have been modified:
        boolean oldAutoComplete = _prefs.getBoolean("autoComplete");
        boolean oldShowSource = _prefs.getBoolean("showSource");
        String oldAutoCompFields = _prefs.get("autoCompleteFields");
        _prefs.putBoolean("autoComplete", autoComplete.isSelected());
        _prefs.put("autoCompleteFields", autoCompFields.getText());
        _prefs.putBoolean("showSource", showSource.isSelected());
        if (autoCompBoth.isSelected()) {
            _prefs.putBoolean("autoCompFF", false);
            _prefs.putBoolean("autoCompLF", false);
        }
        else if (autoCompFF.isSelected()) {
            _prefs.putBoolean("autoCompFF", true);
            _prefs.putBoolean("autoCompLF", false);
        }
        else {
            _prefs.putBoolean("autoCompFF", false);
            _prefs.putBoolean("autoCompLF", true);
        }
        // We need to remove all entry editors from cache if the source panel setting
        // or the autocompletion settings have been changed:
        if ((oldShowSource != showSource.isSelected()) || (oldAutoComplete != autoComplete.isSelected())
                || (!oldAutoCompFields.equals(autoCompFields.getText())) ||
                (oldAutoCompFF != autoCompFF.isSelected()) || (oldAutoCompLF != autoCompLF.isSelected())) {
            for (int j=0; j<_frame.getTabbedPane().getTabCount(); j++) {
	            BasePanel bp = (BasePanel)_frame.getTabbedPane().getComponentAt(j);
	            bp.entryEditors.clear();
            }
        }

    }

    public boolean readyToClose() {
        return true;
    }

	public String getTabName() {
		return Globals.lang("Entry editor");
	}
}
