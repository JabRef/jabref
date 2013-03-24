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

import net.sf.jabref.autocompleter.AbstractAutoCompleter;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class EntryEditorPrefsTab extends JPanel implements PrefsTab {

    private JCheckBox autoOpenForm, showSource,
        defSource, editSource, disableOnMultiple, autoComplete;
    private JRadioButton autoCompBoth, autoCompFF, autoCompLF, 
    	autoCompFirstNameMode_Full, autoCompFirstNameMode_Abbr, autoCompFirstNameMode_Both;
    boolean oldAutoCompFF, oldAutoCompLF,
    	oldAutoCompFModeAbbr, oldAutoCompFModeFull;
    private JSpinner shortestToComplete;

    private JTextField autoCompFields;
    JabRefPreferences _prefs;
    JabRefFrame _frame;
    
    private void setAutoCompleteElementsEnabled(boolean enabled) {
        autoCompFields.setEnabled(enabled);
        autoCompLF.setEnabled(enabled);
        autoCompFF.setEnabled(enabled);
        autoCompBoth.setEnabled(enabled);
        autoCompFirstNameMode_Abbr.setEnabled(enabled);
        autoCompFirstNameMode_Full.setEnabled(enabled);
        autoCompFirstNameMode_Both.setEnabled(enabled);
        shortestToComplete.setEnabled(enabled);
    }

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
        
        shortestToComplete = new JSpinner(new SpinnerNumberModel(prefs.getInt(JabRefPreferences.SHORTEST_TO_COMPLETE), 1, 5, 1));
        
        // allowed name formats
        autoCompFF = new JRadioButton(Globals.lang("Autocomplete names in 'Firstname Lastname' format only"));
        autoCompLF = new JRadioButton(Globals.lang("Autocomplete names in 'Lastname, Firstname' format only"));
        autoCompBoth = new JRadioButton(Globals.lang("Autocomplete names in both formats"));
        ButtonGroup bg = new ButtonGroup();
        bg.add(autoCompLF);
        bg.add(autoCompFF);
        bg.add(autoCompBoth);
        
        // treatment of first name
        autoCompFirstNameMode_Full = new JRadioButton(Globals.lang("Use full firstname whenever possible"));
        autoCompFirstNameMode_Abbr = new JRadioButton(Globals.lang("Use abbreviated firstname whenever possible"));
        autoCompFirstNameMode_Both = new JRadioButton(Globals.lang("Use abbreviated and full firstname"));
        ButtonGroup bg_firstNameMode = new ButtonGroup();
        bg_firstNameMode.add(autoCompFirstNameMode_Full);
        bg_firstNameMode.add(autoCompFirstNameMode_Abbr);
        bg_firstNameMode.add(autoCompFirstNameMode_Both);
        
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
            	setAutoCompleteElementsEnabled(autoComplete.isSelected());
            }
        }
        );

        FormLayout layout = new FormLayout
                ("8dlu, left:pref, 8dlu, fill:150dlu, 4dlu, fill:pref", // 4dlu, left:pref, 4dlu",
                 // rows  1 to 10
                 "pref, 6dlu, pref, 6dlu, pref, 6dlu, pref, 6dlu, pref, 6dlu, " +
                 // rows 11 to 20
                 "pref, 6dlu, pref, 6dlu, pref, 6dlu, pref, pref, pref, pref, " +
                 // rows 21 to 26
                 "6dlu, pref, pref, pref, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.addSeparator(Globals.lang("Editor options"), cc.xyw(1, 1, 5));
        builder.add(autoOpenForm, cc.xy(2, 3));
        builder.add(disableOnMultiple, cc.xy(2, 5));
        builder.add(showSource, cc.xy(2, 7));
        builder.add(defSource, cc.xy(2, 9));
        
        builder.addSeparator(Globals.lang("Autocompletion options"), cc.xyw(1, 11, 5));
        builder.add(autoComplete, cc.xy(2, 13));
        
        DefaultFormBuilder builder3 = new DefaultFormBuilder(new FormLayout("left:pref, 4dlu, fill:150dlu",""));
        JLabel label = new JLabel(Globals.lang("Use autocompletion for the following fields")+":");
        builder3.append(label);
        builder3.append(autoCompFields);
        JLabel label2 = new JLabel(Globals.lang("Autocomplete after following number of characters")+":");
        builder3.append(label2);
        builder3.append(shortestToComplete);
        builder.add(builder3.getPanel(), cc.xyw(2, 15, 3));

        builder.addSeparator(Globals.lang("Name format used for autocompletion"), cc.xyw(2, 17, 4));
        builder.add(autoCompFF, cc.xy(2,18));
        builder.add(autoCompLF, cc.xy(2,19));
        builder.add(autoCompBoth, cc.xy(2,20));
        
        builder.addSeparator(Globals.lang("Treatment of first names"), cc.xyw(2, 22, 4));
        builder.add(autoCompFirstNameMode_Abbr, cc.xy(2,23));
        builder.add(autoCompFirstNameMode_Full, cc.xy(2,24));
        builder.add(autoCompFirstNameMode_Both, cc.xy(2,25));
        
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
        shortestToComplete.setValue(_prefs.getInt(JabRefPreferences.SHORTEST_TO_COMPLETE));
        
        if (_prefs.getBoolean("autoCompFF"))
            autoCompFF.setSelected(true);
        else if (_prefs.getBoolean("autoCompLF"))
            autoCompLF.setSelected(true);
        else
            autoCompBoth.setSelected(true);
        oldAutoCompFF = autoCompFF.isSelected();
        oldAutoCompLF = autoCompLF.isSelected();

        if (_prefs.get(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE).equals(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_ABBR))
        	autoCompFirstNameMode_Abbr.setSelected(true);
        else if (_prefs.get(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE).equals(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_FULL))
        	autoCompFirstNameMode_Full.setSelected(true);
        else
        	autoCompFirstNameMode_Both.setSelected(true);
        // one field less than the option is enough. If one filed changes, another one also changes.
        oldAutoCompFModeAbbr = autoCompFirstNameMode_Abbr.isSelected();
        oldAutoCompFModeFull = autoCompFirstNameMode_Full.isSelected();

        // Two choices only make sense when the source panel is visible:
        defSource.setEnabled(showSource.isSelected());
        editSource.setEnabled(showSource.isSelected());
        // Autocomplete fields is only enabled when autocompletion is:
        setAutoCompleteElementsEnabled(autoComplete.isSelected());
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
        _prefs.putInt(JabRefPreferences.SHORTEST_TO_COMPLETE, (Integer)shortestToComplete.getValue());
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
        if (autoCompFirstNameMode_Abbr.isSelected())
        	_prefs.put(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE, JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_ABBR);
        else if (autoCompFirstNameMode_Full.isSelected())
        	_prefs.put(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE, JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_FULL);
        else
        	_prefs.put(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE, JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_BOTH);
        
        // We need to remove all entry editors from cache if the source panel setting
        // or the autocompletion settings have been changed:
        if ((oldShowSource != showSource.isSelected()) || (oldAutoComplete != autoComplete.isSelected())
                || (!oldAutoCompFields.equals(autoCompFields.getText())) ||
                (oldAutoCompFF != autoCompFF.isSelected()) || (oldAutoCompLF != autoCompLF.isSelected()) ||
                (oldAutoCompFModeAbbr != autoCompFirstNameMode_Abbr.isSelected()) ||
                (oldAutoCompFModeFull != autoCompFirstNameMode_Full.isSelected())) {
            for (int j=0; j<_frame.getTabbedPane().getTabCount(); j++) {
	            BasePanel bp = (BasePanel)_frame.getTabbedPane().getComponentAt(j);
	            bp.entryEditors.clear();
            }
        }
        // the autocompleter has to be updated to the new min length to complete 
        AbstractAutoCompleter.SHORTEST_TO_COMPLETE = (Integer)shortestToComplete.getValue();
    }

    public boolean readyToClose() {
        return true;
    }

	public String getTabName() {
		return Globals.lang("Entry editor");
	}
}
