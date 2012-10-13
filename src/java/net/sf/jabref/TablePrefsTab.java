/*  Copyright (C) 2003-2012 JabRef contributors.
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.specialfields.SpecialFieldsUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

class TablePrefsTab extends JPanel implements PrefsTab {

	JabRefPreferences _prefs;

    private JCheckBox autoResizeMode, priDesc, secDesc, terDesc, floatMarked, pdfColumn, urlColumn,
	fileColumn, arxivColumn;

	private JRadioButton namesAsIs, namesFf, namesFl, namesNatbib, abbrNames, noAbbrNames,
		lastNamesOnly;

	private JComboBox priSort, secSort, terSort;

	/*** begin: special fields ***/
	private JTextField priField, secField, terField, numericFields;
	private JCheckBox specialFieldsEnabled, rankingColumn, compactRankingColumn, qualityColumn, priorityColumn, relevanceColumn;
	private JRadioButton syncKeywords, writeSpecialFields;
	private boolean oldSpecialFieldsEnabled, oldRankingColumn, oldCompcatRankingColumn, oldQualityColumn, oldPriorityColumn, oldRelevanceColumn, oldSyncKeyWords, oldWriteSpecialFields;
	private final JButton hlb; 
	/*** end: special fields ***/

	/**
	 * Customization of external program paths.
	 * 
	 * @param prefs
	 *            a <code>JabRefPreferences</code> value
	 */
	public TablePrefsTab(JabRefPreferences prefs, JabRefFrame frame) {
		_prefs = prefs;
		setLayout(new BorderLayout());

		/**
		 * Added Bibtexkey to combobox.
		 * 
		 * [ 1540646 ] default sort order: bibtexkey
		 * 
		 * http://sourceforge.net/tracker/index.php?func=detail&aid=1540646&group_id=92314&atid=600306
		 */
		Vector<String> v = new Vector<String>(Arrays.asList(BibtexFields.getAllFieldNames()));
		v.add(BibtexFields.KEY_FIELD);
		Collections.sort(v);
		Object[] allPlusKey = v.toArray();
		priSort = new JComboBox(allPlusKey);
		secSort = new JComboBox(allPlusKey);
		terSort = new JComboBox(allPlusKey);

		autoResizeMode = new JCheckBox(Globals.lang("Fit table horizontally on screen"));

		namesAsIs = new JRadioButton(Globals.lang("Show names unchanged"));
		namesFf = new JRadioButton(Globals.lang("Show 'Firstname Lastname'"));
		namesFl = new JRadioButton(Globals.lang("Show 'Lastname, Firstname'"));
		namesNatbib = new JRadioButton(Globals.lang("Natbib style"));
		noAbbrNames = new JRadioButton(Globals.lang("Do not abbreviate names"));
		abbrNames = new JRadioButton(Globals.lang("Abbreviate names"));
		lastNamesOnly = new JRadioButton(Globals.lang("Show last names only"));
        fileColumn = new JCheckBox(Globals.lang("Show file column"));
        pdfColumn = new JCheckBox(Globals.lang("Show PDF/PS column"));
		urlColumn = new JCheckBox(Globals.lang("Show URL/DOI column"));
		arxivColumn = new JCheckBox(Globals.lang("Show ArXiv column"));

		floatMarked = new JCheckBox(Globals.lang("Float marked entries"));

		priField = new JTextField(10);
		secField = new JTextField(10);
		terField = new JTextField(10);

        numericFields = new JTextField(30);

		priSort.insertItemAt(Globals.lang("<select>"), 0);
		secSort.insertItemAt(Globals.lang("<select>"), 0);
		terSort.insertItemAt(Globals.lang("<select>"), 0);

		priSort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (priSort.getSelectedIndex() > 0) {
                    priField.setText(priSort.getSelectedItem().toString());
                    priSort.setSelectedIndex(0);
				}
			}
		});
		secSort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (secSort.getSelectedIndex() > 0) {
                    secField.setText(secSort.getSelectedItem().toString());
                    secSort.setSelectedIndex(0);
				}
			}
		});
		terSort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (terSort.getSelectedIndex() > 0) {
					terField.setText(terSort.getSelectedItem().toString());
                    terSort.setSelectedIndex(0);
				}
			}
		});

		ButtonGroup bg = new ButtonGroup();
		bg.add(namesAsIs);
		bg.add(namesNatbib);
		bg.add(namesFf);
		bg.add(namesFl);
		ButtonGroup bg2 = new ButtonGroup();
		bg2.add(lastNamesOnly);
		bg2.add(abbrNames);
		bg2.add(noAbbrNames);
		priDesc = new JCheckBox(Globals.lang("Descending"));
		secDesc = new JCheckBox(Globals.lang("Descending"));
		terDesc = new JCheckBox(Globals.lang("Descending"));

		FormLayout layout = new FormLayout(
			"1dlu, 8dlu, left:pref, 4dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, fill:pref", "");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		JLabel lab;
		JPanel pan = new JPanel();
		
		/*** begin: special table columns and special fields ***/

		HelpAction help = new HelpAction(frame.helpDiag, GUIGlobals.specialFieldsHelp, "Help on key patterns");
	    hlb = new JButton(GUIGlobals.getImage("helpSmall"));
	    hlb.setToolTipText(Globals.lang("Help on special fields"));
	    hlb.addActionListener(help);
		
		specialFieldsEnabled = new JCheckBox(Globals.lang("Enable special fields"));
//		.concat(". ").concat(Globals.lang("You must restart JabRef for this to come into effect.")));
		specialFieldsEnabled.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				boolean isEnabled = specialFieldsEnabled.isSelected();
				rankingColumn.setEnabled(isEnabled);
				compactRankingColumn.setEnabled(isEnabled?rankingColumn.isSelected():false);
				qualityColumn.setEnabled(isEnabled);
				priorityColumn.setEnabled(isEnabled);
				relevanceColumn.setEnabled(isEnabled);
				syncKeywords.setEnabled(isEnabled);
				writeSpecialFields.setEnabled(isEnabled);
			}
		});
		rankingColumn = new JCheckBox(Globals.lang("Show ranking"));
		rankingColumn.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				compactRankingColumn.setEnabled(rankingColumn.isSelected());
			}
		});
		compactRankingColumn = new JCheckBox(Globals.lang("Compact ranking"));
		qualityColumn = new JCheckBox(Globals.lang("Show quality"));
		priorityColumn = new JCheckBox(Globals.lang("Show priority"));
		relevanceColumn = new JCheckBox(Globals.lang("Show relevance"));
		
		// "sync keywords" and "write special" fields may be configured mutually exclusive only
		// The implementation supports all combinations (TRUE+TRUE and FALSE+FALSE, even if the latter does not make sense)
		// To avoid confusion, we opted to make the setting mutually exclusive
		syncKeywords = new JRadioButton(Globals.lang("Synchronize with keywords"));
		writeSpecialFields = new JRadioButton(Globals.lang("Write values of special fields as separate fields to BibTeX"));
		ButtonGroup group = new ButtonGroup();
		group.add(syncKeywords);
		group.add(writeSpecialFields);
		
		builder.appendSeparator(Globals.lang("Special table columns"));
		builder.nextLine();
		builder.append(pan);

		DefaultFormBuilder specialTableColumnsBuilder = new DefaultFormBuilder(new FormLayout(
				"8dlu, 8dlu, 8cm, 8dlu, left:pref", "pref, pref, pref, pref, pref, pref, pref, pref, pref"));
        CellConstraints cc = new CellConstraints();
		
        specialTableColumnsBuilder.add(specialFieldsEnabled, cc.xyw(1, 1, 3));
        specialTableColumnsBuilder.add(rankingColumn, cc.xyw(2, 2, 2));
        specialTableColumnsBuilder.add(compactRankingColumn, cc.xy(3, 3));
        specialTableColumnsBuilder.add(relevanceColumn, cc.xyw(2, 4, 2));
        specialTableColumnsBuilder.add(qualityColumn, cc.xyw(2, 5, 2));
        specialTableColumnsBuilder.add(priorityColumn, cc.xyw(2, 6, 2));
        specialTableColumnsBuilder.add(syncKeywords, cc.xyw(2, 7, 2));
        specialTableColumnsBuilder.add(writeSpecialFields, cc.xyw(2, 8, 2));
		specialTableColumnsBuilder.add(hlb, cc.xyw(1, 9, 2));

		specialTableColumnsBuilder.add(fileColumn, cc.xy(5, 1));	
		specialTableColumnsBuilder.add(pdfColumn, cc.xy(5, 2));	
		specialTableColumnsBuilder.add(urlColumn, cc.xy(5, 3));	
		specialTableColumnsBuilder.add(arxivColumn, cc.xy(5, 4));	

		builder.append(specialTableColumnsBuilder.getPanel());
		builder.nextLine();

		/*** end: special table columns and special fields ***/
		
		builder.appendSeparator(Globals.lang("Format of author and editor names"));
		DefaultFormBuilder nameBuilder = new DefaultFormBuilder(new FormLayout(
			"left:pref, 8dlu, left:pref", ""));

		nameBuilder.append(namesAsIs);
		nameBuilder.append(noAbbrNames);
		nameBuilder.nextLine();
		nameBuilder.append(namesFf);
		nameBuilder.append(abbrNames);
		nameBuilder.nextLine();
		nameBuilder.append(namesFl);
		nameBuilder.append(lastNamesOnly);
		nameBuilder.nextLine();
		nameBuilder.append(namesNatbib);
		builder.append(pan);
		builder.append(nameBuilder.getPanel());
		builder.nextLine();
		// builder.append(pan); builder.append(noAbbrNames); builder.nextLine();
		// builder.append(pan); builder.append(abbrNames); builder.nextLine();
		// builder.append(pan); builder.append(lastNamesOnly);
		// builder.nextLine();

		builder.appendSeparator(Globals.lang("Default sort criteria"));
		// Create a new panel with its own FormLayout for these items:
		FormLayout layout2 = new FormLayout(
			"left:pref, 8dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, left:pref", "");
		DefaultFormBuilder builder2 = new DefaultFormBuilder(layout2);
		lab = new JLabel(Globals.lang("Primary sort criterion"));
		builder2.append(lab);
		builder2.append(priSort);
		builder2.append(priField);
		builder2.append(priDesc);
		builder2.nextLine();
		lab = new JLabel(Globals.lang("Secondary sort criterion"));
		builder2.append(lab);
		builder2.append(secSort);
		builder2.append(secField);
		builder2.append(secDesc);
		builder2.nextLine();
		lab = new JLabel(Globals.lang("Tertiary sort criterion"));
		builder2.append(lab);
		builder2.append(terSort);
		builder2.append(terField);
		builder2.append(terDesc);
		builder.nextLine();
		builder.append(pan);
		builder.append(builder2.getPanel());
		builder.nextLine();
		builder.append(pan);
		builder.append(floatMarked);
		builder.nextLine();
        builder.append(pan);
        builder2 = new DefaultFormBuilder(new FormLayout("left:pref, 8dlu, fill:pref",""));
        builder2.append(Globals.lang("Sort the following fields as numeric fields")+":");
        builder2.append(numericFields);
        builder.append(builder2.getPanel(), 5);
        builder.nextLine();
		builder.appendSeparator(Globals.lang("General"));
		builder.append(pan);
		builder.append(autoResizeMode);
		builder.nextLine();

		pan = builder.getPanel();
		pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(pan, BorderLayout.CENTER);

		namesNatbib.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent changeEvent) {
				abbrNames.setEnabled(!namesNatbib.isSelected());
				lastNamesOnly.setEnabled(!namesNatbib.isSelected());
				noAbbrNames.setEnabled(!namesNatbib.isSelected());
			}
		});
	}

	public void setValues() {
		autoResizeMode
			.setSelected((_prefs.getInt("autoResizeMode") == JTable.AUTO_RESIZE_ALL_COLUMNS));
		fileColumn.setSelected(_prefs.getBoolean("fileColumn"));
        pdfColumn.setSelected(_prefs.getBoolean("pdfColumn"));
		urlColumn.setSelected(_prefs.getBoolean("urlColumn"));
        fileColumn.setSelected(_prefs.getBoolean("fileColumn"));
        arxivColumn.setSelected(_prefs.getBoolean("arxivColumn"));

		priField.setText(_prefs.get("priSort"));
		secField.setText(_prefs.get("secSort"));
		terField.setText(_prefs.get("terSort"));
		priSort.setSelectedIndex(0);
		secSort.setSelectedIndex(0);
		terSort.setSelectedIndex(0);

		if (_prefs.getBoolean("namesAsIs"))
			namesAsIs.setSelected(true);
		else if (_prefs.getBoolean("namesFf"))
			namesFf.setSelected(true);
		else if (_prefs.getBoolean("namesNatbib"))
			namesNatbib.setSelected(true);
		else
			namesFl.setSelected(true);
		if (_prefs.getBoolean("abbrAuthorNames"))
			abbrNames.setSelected(true);
		else if (_prefs.getBoolean("namesLastOnly"))
			lastNamesOnly.setSelected(true);
		else
			noAbbrNames.setSelected(true);
		priDesc.setSelected(_prefs.getBoolean("priDescending"));
		secDesc.setSelected(_prefs.getBoolean("secDescending"));
		terDesc.setSelected(_prefs.getBoolean("terDescending"));

		floatMarked.setSelected(_prefs.getBoolean("floatMarkedEntries"));

		abbrNames.setEnabled(!namesNatbib.isSelected());
		lastNamesOnly.setEnabled(!namesNatbib.isSelected());
		noAbbrNames.setEnabled(!namesNatbib.isSelected());

        String numF = _prefs.get("numericFields");
        if (numF == null)
            numericFields.setText("");
        else
            numericFields.setText(numF);

        /*** begin: special fields ***/

        oldRankingColumn = _prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING);
        rankingColumn.setSelected(oldRankingColumn);
        
        oldCompcatRankingColumn = _prefs.getBoolean(SpecialFieldsUtils.PREF_RANKING_COMPACT);
        compactRankingColumn.setSelected(oldCompcatRankingColumn);
		
        oldQualityColumn = _prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY);
        qualityColumn.setSelected(oldQualityColumn);
        
		oldPriorityColumn = _prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY);
        priorityColumn.setSelected(oldPriorityColumn);
		
		oldRelevanceColumn = _prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE);
        relevanceColumn.setSelected(oldRelevanceColumn);
		
		oldSyncKeyWords = _prefs.getBoolean(SpecialFieldsUtils.PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS);
		syncKeywords.setSelected(oldSyncKeyWords);
		
		oldWriteSpecialFields = _prefs.getBoolean(SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS);
		writeSpecialFields.setSelected(oldWriteSpecialFields);

		// has to be called as last to correctly enable/disable the other settings
		oldSpecialFieldsEnabled = _prefs.getBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED);
		specialFieldsEnabled.setSelected(oldSpecialFieldsEnabled);
		
        /*** end: special fields ***/
	}

	/**
	 * Store changes to table preferences. This method is called when the user
	 * clicks Ok.
	 * 
	 */
	public void storeSettings() {

		_prefs.putBoolean("namesAsIs", namesAsIs.isSelected());
		_prefs.putBoolean("namesFf", namesFf.isSelected());
		_prefs.putBoolean("namesNatbib", namesNatbib.isSelected());
		_prefs.putBoolean("namesLastOnly", lastNamesOnly.isSelected());
		_prefs.putBoolean("abbrAuthorNames", abbrNames.isSelected());

        _prefs.putBoolean("fileColumn", fileColumn.isSelected());
        _prefs.putBoolean("pdfColumn", pdfColumn.isSelected());
		_prefs.putBoolean("urlColumn", urlColumn.isSelected());
		_prefs.putBoolean("arxivColumn", arxivColumn.isSelected());
		_prefs.putInt("autoResizeMode",
			autoResizeMode.isSelected() ? JTable.AUTO_RESIZE_ALL_COLUMNS : JTable.AUTO_RESIZE_OFF);
		_prefs.putBoolean("priDescending", priDesc.isSelected());
		_prefs.putBoolean("secDescending", secDesc.isSelected());
		_prefs.putBoolean("terDescending", terDesc.isSelected());
		// _prefs.put("secSort",
		// GUIGlobals.ALL_FIELDS[secSort.getSelectedIndex()]);
		// _prefs.put("terSort",
		// GUIGlobals.ALL_FIELDS[terSort.getSelectedIndex()]);
		_prefs.put("priSort", priField.getText().toLowerCase().trim());
		_prefs.put("secSort", secField.getText().toLowerCase().trim());
		_prefs.put("terSort", terField.getText().toLowerCase().trim());

		_prefs.putBoolean("floatMarkedEntries", floatMarked.isSelected());
		// updatefont

        String oldVal = _prefs.get("numericFields");
        String newVal = numericFields.getText().trim();
        if (newVal.length() == 0)
            newVal = null;
        if (((newVal != null) && (oldVal == null))
                || ((newVal == null) && (oldVal != null))
                || ((newVal != null) && !newVal.equals(oldVal))) {
            _prefs.put("numericFields", newVal);
            BibtexFields.setNumericFieldsFromPrefs();
        }

        /*** begin: special fields ***/
        
		boolean 
		newSpecialFieldsEnabled = specialFieldsEnabled.isSelected(),
		newRankingColumn = rankingColumn.isSelected(),
		newCompactRankingColumn = compactRankingColumn.isSelected(),
		newQualityColumn = qualityColumn.isSelected(), 
		newPriorityColumn = priorityColumn.isSelected(), 
		newRelevanceColumn = relevanceColumn.isSelected(), 
		newSyncKeyWords = syncKeywords.isSelected(), 
		newWriteSpecialFields = writeSpecialFields.isSelected();
		
		boolean restartRequired = false;
		restartRequired = (oldSpecialFieldsEnabled != newSpecialFieldsEnabled) ||
				(oldRankingColumn != newRankingColumn) ||
				(oldCompcatRankingColumn != newCompactRankingColumn) ||
				(oldQualityColumn != newQualityColumn) ||
				(oldPriorityColumn != newPriorityColumn) ||
				(oldRelevanceColumn != newRelevanceColumn);
		if (restartRequired) {
	        JOptionPane.showMessageDialog(null, 
	        		Globals.lang("You have changed settings for special fields.")
	        		.concat(" ")
	        		.concat(Globals.lang("You must restart JabRef for this to come into effect.")),
	        		Globals.lang("Changed special field settings"),
	        		JOptionPane.WARNING_MESSAGE);
		}
		
		// restart required implies that the settings have been changed
		// the seetings need to be stored
		if (restartRequired ||
				(oldSyncKeyWords != newSyncKeyWords) ||
				(oldWriteSpecialFields != newWriteSpecialFields)) {
			_prefs.putBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED, newSpecialFieldsEnabled);
			_prefs.putBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING, newRankingColumn);
			_prefs.putBoolean(SpecialFieldsUtils.PREF_RANKING_COMPACT, newCompactRankingColumn);
			_prefs.putBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY, newPriorityColumn);
			_prefs.putBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY, newQualityColumn);
			_prefs.putBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE, newRelevanceColumn);
			_prefs.putBoolean(SpecialFieldsUtils.PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS, newSyncKeyWords);
			_prefs.putBoolean(SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS, newWriteSpecialFields);
		}
		
        /*** end: special fields ***/
	}

	public boolean readyToClose() {
		return true;
	}

	public String getTabName() {
		return Globals.lang("Entry table");
	}
}
