/*  Copyright (C) 2012 JabRef contributors.
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
package net.sf.jabref.specialfields;

import java.awt.BorderLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.HelpAction;
import net.sf.jabref.HelpDialog;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.PrefsTab;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class SpecialFieldsPrefsTab extends JPanel implements PrefsTab {
	private JCheckBox specialFieldsEnabled, rankingColumn, qualityColumn, priorityColumn, relevanceColumn, syncKeywords, writeSpecialFields;
	
	private final JabRefPreferences prefs;

	private boolean oldSpecialFieldsEnabled, oldRankingColumn, oldQualityColumn, oldPriorityColumn, oldRelevanceColumn, oldSyncKeyWords, oldWriteSpecialFields;

	private final JButton hlb; 

	public SpecialFieldsPrefsTab(JabRefPreferences prefs, HelpDialog helpDiag) {
		this.prefs = prefs;
		HelpAction help = new HelpAction(helpDiag, GUIGlobals.specialFieldsHelp, "Help on key patterns");
	    hlb = new JButton(GUIGlobals.getImage("helpSmall"));
	    hlb.setToolTipText(Globals.lang("Help on special fields"));
	    hlb.addActionListener(help);
		
		specialFieldsEnabled = new JCheckBox(Globals.lang("Enable special fields"));
//		.concat(". ").concat(Globals.lang("You must restart JabRef for this to come into effect.")));
		specialFieldsEnabled.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				boolean isEnabled = specialFieldsEnabled.isSelected();
				rankingColumn.setEnabled(isEnabled);
				qualityColumn.setEnabled(isEnabled);
				priorityColumn.setEnabled(isEnabled);
				relevanceColumn.setEnabled(isEnabled);
				syncKeywords.setEnabled(isEnabled);
				writeSpecialFields.setEnabled(isEnabled);
			}
		});
		rankingColumn = new JCheckBox(Globals.lang("Show ranking"));	
		qualityColumn = new JCheckBox(Globals.lang("Show quality"));	
		priorityColumn = new JCheckBox(Globals.lang("Show priority"));
		relevanceColumn = new JCheckBox(Globals.lang("Show relevance"));
		syncKeywords = new JCheckBox(Globals.lang("Synchronize with keywords"));
		writeSpecialFields = new JCheckBox(Globals.lang("Write values of special fields as separate fields to BibTeX"));
		
		FormLayout layout = new FormLayout("12dlu pref", "pref pref pref pref pref pref pref pref");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();
		
		builder.add(specialFieldsEnabled, cc.xyw(1, 1, 2));
		builder.add(rankingColumn, cc.xy(2, 2));
		builder.add(relevanceColumn, cc.xy(2, 3));
		builder.add(qualityColumn, cc.xy(2, 4));
		builder.add(priorityColumn, cc.xy(2, 5));
		builder.add(syncKeywords, cc.xy(2, 6));
		builder.add(writeSpecialFields, cc.xy(2, 7));
	    builder.add(hlb, cc.xy(1, 8));

		setLayout(new BorderLayout());
		JPanel panel = builder.getPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(panel, BorderLayout.CENTER);
	}

	public void setValues() {
		oldRankingColumn = prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING);
        rankingColumn.setSelected(oldRankingColumn);
		
        oldQualityColumn = prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY);
        qualityColumn.setSelected(oldQualityColumn);
        
		oldPriorityColumn = prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY);
        priorityColumn.setSelected(oldPriorityColumn);
		
		oldRelevanceColumn = prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE);
        relevanceColumn.setSelected(oldRelevanceColumn);
		
		oldSyncKeyWords = prefs.getBoolean(SpecialFieldsUtils.PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS);
		syncKeywords.setSelected(oldSyncKeyWords);
		
		oldWriteSpecialFields = prefs.getBoolean(SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS);
		writeSpecialFields.setSelected(oldWriteSpecialFields);

		// has to be called as last to correctly enable/disable the other settings
		oldSpecialFieldsEnabled = prefs.getBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED);
		specialFieldsEnabled.setSelected(oldSpecialFieldsEnabled);
}

	public void storeSettings() {
		boolean 
		newSpecialFieldsEnabled = specialFieldsEnabled.isSelected(),
		newRankingColumn = rankingColumn.isSelected(),
		newQualityColumn = qualityColumn.isSelected(), 
		newPriorityColumn = priorityColumn.isSelected(), 
		newRelevanceColumn = relevanceColumn.isSelected(), 
		newSyncKeyWords = syncKeywords.isSelected(), 
		newWriteSpecialFields = writeSpecialFields.isSelected();
		
		if ((oldSpecialFieldsEnabled != newSpecialFieldsEnabled) ||
				(oldRankingColumn != newRankingColumn) ||
				(oldQualityColumn != newQualityColumn) ||
				(oldPriorityColumn != newPriorityColumn) ||
				(oldRelevanceColumn != newRelevanceColumn) ||
				(oldSyncKeyWords != newSyncKeyWords) ||
				(oldWriteSpecialFields != newWriteSpecialFields)) {
			
	        JOptionPane.showMessageDialog(null, 
	        		Globals.lang("You have changed settings for special fields.")
	        		.concat(" ")
	        		.concat(Globals.lang("You must restart JabRef for this to come into effect.")),
	        		Globals.lang("Changed special field settings"),
	        		JOptionPane.WARNING_MESSAGE);
	
			prefs.putBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED, newSpecialFieldsEnabled);
			prefs.putBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING, newRankingColumn);
			prefs.putBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY, newQualityColumn);
			prefs.putBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY, newPriorityColumn);
			prefs.putBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE, newRelevanceColumn);
			prefs.putBoolean(SpecialFieldsUtils.PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS, newSyncKeyWords);
			prefs.putBoolean(SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS, newWriteSpecialFields);
		}
	}

	public boolean readyToClose() {
		return true;
	}

	public String getTabName() {
		return Globals.lang("Special Fields");
	}

}
