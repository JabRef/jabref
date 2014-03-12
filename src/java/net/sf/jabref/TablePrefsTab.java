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

    private JCheckBox autoResizeMode, priDesc, secDesc, terDesc, floatMarked;

	private JRadioButton namesAsIs, namesFf, namesFl, namesNatbib, abbrNames, noAbbrNames,
		lastNamesOnly;

	private JTextField priField, secField, terField, numericFields;
	private JComboBox priSort, secSort, terSort;

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

		priField.setText(_prefs.get(JabRefPreferences.PRIMARY_SORT_FIELD));
		secField.setText(_prefs.get(JabRefPreferences.SECONDARY_SORT_FIELD));
		terField.setText(_prefs.get(JabRefPreferences.TERTIARY_SORT_FIELD));
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
		priDesc.setSelected(_prefs.getBoolean(JabRefPreferences.PRIMARY_SORT_DESCENDING));
		secDesc.setSelected(_prefs.getBoolean(JabRefPreferences.SECONDARY_SORT_DESCENDING));
		terDesc.setSelected(_prefs.getBoolean(JabRefPreferences.TERTIARY_SORT_DESCENDING));

		floatMarked.setSelected(_prefs.getBoolean("floatMarkedEntries"));

		abbrNames.setEnabled(!namesNatbib.isSelected());
		lastNamesOnly.setEnabled(!namesNatbib.isSelected());
		noAbbrNames.setEnabled(!namesNatbib.isSelected());

        String numF = _prefs.get("numericFields");
        if (numF == null)
            numericFields.setText("");
        else
            numericFields.setText(numF);

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

		_prefs.putInt("autoResizeMode",
			autoResizeMode.isSelected() ? JTable.AUTO_RESIZE_ALL_COLUMNS : JTable.AUTO_RESIZE_OFF);
		_prefs.putBoolean(JabRefPreferences.PRIMARY_SORT_DESCENDING, priDesc.isSelected());
		_prefs.putBoolean(JabRefPreferences.SECONDARY_SORT_DESCENDING, secDesc.isSelected());
		_prefs.putBoolean(JabRefPreferences.TERTIARY_SORT_DESCENDING, terDesc.isSelected());
		// _prefs.put(JabRefPreferences.SECONDARY_SORT_FIELD,
		// GUIGlobals.ALL_FIELDS[secSort.getSelectedIndex()]);
		// _prefs.put(JabRefPreferences.TERTIARY_SORT_FIELD,
		// GUIGlobals.ALL_FIELDS[terSort.getSelectedIndex()]);
		_prefs.put(JabRefPreferences.PRIMARY_SORT_FIELD, priField.getText().toLowerCase().trim());
		_prefs.put(JabRefPreferences.SECONDARY_SORT_FIELD, secField.getText().toLowerCase().trim());
		_prefs.put(JabRefPreferences.TERTIARY_SORT_FIELD, terField.getText().toLowerCase().trim());

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

	}

	public boolean readyToClose() {
		return true;
	}

	public String getTabName() {
		return Globals.lang("Entry table");
	}
}
