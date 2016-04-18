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
package net.sf.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JTextField;

import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

class TablePrefsTab extends JPanel implements PrefsTab {

    private final JabRefPreferences prefs;

    private final JCheckBox autoResizeMode;
    private final JCheckBox priDesc;
    private final JCheckBox secDesc;
    private final JCheckBox terDesc;
    private final JCheckBox floatMarked;

    private final JRadioButton namesAsIs;
    private final JRadioButton namesFf;
    private final JRadioButton namesFl;
    private final JRadioButton namesNatbib;
    private final JRadioButton abbrNames;
    private final JRadioButton noAbbrNames;
    private final JRadioButton lastNamesOnly;

    private final JTextField priField;
    private final JTextField secField;
    private final JTextField terField;
    private final JTextField numericFields;
    private final JComboBox<String> priSort;
    private final JComboBox<String> secSort;
    private final JComboBox<String> terSort;


    /**
     * Customization of external program paths.
     *
     * @param prefs
     *            a <code>JabRefPreferences</code> value
     */
    public TablePrefsTab(JabRefPreferences prefs) {
        this.prefs = prefs;
        setLayout(new BorderLayout());

        /**
         * Added Bibtexkey to combobox.
         *
         * [ 1540646 ] default sort order: bibtexkey
         *
         * http://sourceforge.net/tracker/index.php?func=detail&aid=1540646&group_id=92314&atid=600306
         */
        List<String> fieldNames = new ArrayList<>(InternalBibtexFields.getAllFieldNames());
        fieldNames.add(BibEntry.KEY_FIELD);
        Collections.sort(fieldNames);
        String[] allPlusKey = fieldNames.toArray(new String[fieldNames.size()]);
        priSort = new JComboBox<>(allPlusKey);
        secSort = new JComboBox<>(allPlusKey);
        terSort = new JComboBox<>(allPlusKey);

        autoResizeMode = new JCheckBox(Localization.lang("Fit table horizontally on screen"));

        namesAsIs = new JRadioButton(Localization.lang("Show names unchanged"));
        namesFf = new JRadioButton(Localization.lang("Show 'Firstname Lastname'"));
        namesFl = new JRadioButton(Localization.lang("Show 'Lastname, Firstname'"));
        namesNatbib = new JRadioButton(Localization.lang("Natbib style"));
        noAbbrNames = new JRadioButton(Localization.lang("Do not abbreviate names"));
        abbrNames = new JRadioButton(Localization.lang("Abbreviate names"));
        lastNamesOnly = new JRadioButton(Localization.lang("Show last names only"));

        floatMarked = new JCheckBox(Localization.lang("Float marked entries"));

        priField = new JTextField(10);
        secField = new JTextField(10);
        terField = new JTextField(10);

        numericFields = new JTextField(30);

        priSort.insertItemAt(Localization.lang("<select>"), 0);
        secSort.insertItemAt(Localization.lang("<select>"), 0);
        terSort.insertItemAt(Localization.lang("<select>"), 0);

        priSort.addActionListener(e -> {
            if (priSort.getSelectedIndex() > 0) {
                priField.setText(priSort.getSelectedItem().toString());
                priSort.setSelectedIndex(0);
            }
        });
        secSort.addActionListener(e -> {
            if (secSort.getSelectedIndex() > 0) {
                secField.setText(secSort.getSelectedItem().toString());
                secSort.setSelectedIndex(0);
            }
        });
        terSort.addActionListener(e -> {
            if (terSort.getSelectedIndex() > 0) {
                terField.setText(terSort.getSelectedItem().toString());
                terSort.setSelectedIndex(0);
            }
        });

        ButtonGroup nameStyle = new ButtonGroup();
        nameStyle.add(namesAsIs);
        nameStyle.add(namesNatbib);
        nameStyle.add(namesFf);
        nameStyle.add(namesFl);
        ButtonGroup nameAbbrev = new ButtonGroup();
        nameAbbrev.add(lastNamesOnly);
        nameAbbrev.add(abbrNames);
        nameAbbrev.add(noAbbrNames);
        priDesc = new JCheckBox(Localization.lang("Descending"));
        secDesc = new JCheckBox(Localization.lang("Descending"));
        terDesc = new JCheckBox(Localization.lang("Descending"));

        FormLayout layout = new FormLayout(
                "1dlu, 8dlu, left:pref, 4dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, fill:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        JLabel lab;
        JPanel pan = new JPanel();

        builder.appendSeparator(Localization.lang("Format of author and editor names"));
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

        builder.appendSeparator(Localization.lang("Default sort criteria"));
        // Create a new panel with its own FormLayout for these items:
        FormLayout layout2 = new FormLayout(
                "left:pref, 8dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, left:pref", "");
        DefaultFormBuilder builder2 = new DefaultFormBuilder(layout2);
        lab = new JLabel(Localization.lang("Primary sort criterion"));
        builder2.append(lab);
        builder2.append(priSort);
        builder2.append(priField);
        builder2.append(priDesc);
        builder2.nextLine();
        lab = new JLabel(Localization.lang("Secondary sort criterion"));
        builder2.append(lab);
        builder2.append(secSort);
        builder2.append(secField);
        builder2.append(secDesc);
        builder2.nextLine();
        lab = new JLabel(Localization.lang("Tertiary sort criterion"));
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
        builder2 = new DefaultFormBuilder(new FormLayout("left:pref, 8dlu, fill:pref", ""));
        builder2.append(Localization.lang("Sort the following fields as numeric fields") + ':');
        builder2.append(numericFields);
        builder.append(builder2.getPanel(), 5);
        builder.nextLine();
        builder.appendSeparator(Localization.lang("General"));
        builder.append(pan);
        builder.append(autoResizeMode);
        builder.nextLine();

        pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);

        namesNatbib.addChangeListener(e -> {
            abbrNames.setEnabled(!namesNatbib.isSelected());
            lastNamesOnly.setEnabled(!namesNatbib.isSelected());
            noAbbrNames.setEnabled(!namesNatbib.isSelected());
        });
    }

    @Override
    public void setValues() {
        autoResizeMode
        .setSelected(prefs.getInt(JabRefPreferences.AUTO_RESIZE_MODE) == JTable.AUTO_RESIZE_ALL_COLUMNS);

        priField.setText(prefs.get(JabRefPreferences.TABLE_PRIMARY_SORT_FIELD));
        secField.setText(prefs.get(JabRefPreferences.TABLE_SECONDARY_SORT_FIELD));
        terField.setText(prefs.get(JabRefPreferences.TABLE_TERTIARY_SORT_FIELD));
        priSort.setSelectedIndex(0);
        secSort.setSelectedIndex(0);
        terSort.setSelectedIndex(0);

        if (prefs.getBoolean(JabRefPreferences.NAMES_AS_IS)) {
            namesAsIs.setSelected(true);
        } else if (prefs.getBoolean(JabRefPreferences.NAMES_FIRST_LAST)) {
            namesFf.setSelected(true);
        } else if (prefs.getBoolean(JabRefPreferences.NAMES_NATBIB)) {
            namesNatbib.setSelected(true);
        } else {
            namesFl.setSelected(true);
        }
        if (prefs.getBoolean(JabRefPreferences.ABBR_AUTHOR_NAMES)) {
            abbrNames.setSelected(true);
        } else if (prefs.getBoolean(JabRefPreferences.NAMES_LAST_ONLY)) {
            lastNamesOnly.setSelected(true);
        } else {
            noAbbrNames.setSelected(true);
        }
        priDesc.setSelected(prefs.getBoolean(JabRefPreferences.TABLE_PRIMARY_SORT_DESCENDING));
        secDesc.setSelected(prefs.getBoolean(JabRefPreferences.TABLE_SECONDARY_SORT_DESCENDING));
        terDesc.setSelected(prefs.getBoolean(JabRefPreferences.TABLE_TERTIARY_SORT_DESCENDING));

        floatMarked.setSelected(prefs.getBoolean(JabRefPreferences.FLOAT_MARKED_ENTRIES));

        abbrNames.setEnabled(!namesNatbib.isSelected());
        lastNamesOnly.setEnabled(!namesNatbib.isSelected());
        noAbbrNames.setEnabled(!namesNatbib.isSelected());

        String numF = prefs.get(JabRefPreferences.NUMERIC_FIELDS);
        if (numF == null) {
            numericFields.setText("");
        } else {
            numericFields.setText(numF);
        }

    }

    /**
     * Store changes to table preferences. This method is called when the user
     * clicks Ok.
     *
     */
    @Override
    public void storeSettings() {

        prefs.putBoolean(JabRefPreferences.NAMES_AS_IS, namesAsIs.isSelected());
        prefs.putBoolean(JabRefPreferences.NAMES_FIRST_LAST, namesFf.isSelected());
        prefs.putBoolean(JabRefPreferences.NAMES_NATBIB, namesNatbib.isSelected());
        prefs.putBoolean(JabRefPreferences.NAMES_LAST_ONLY, lastNamesOnly.isSelected());
        prefs.putBoolean(JabRefPreferences.ABBR_AUTHOR_NAMES, abbrNames.isSelected());

        prefs.putInt(JabRefPreferences.AUTO_RESIZE_MODE,
                autoResizeMode.isSelected() ? JTable.AUTO_RESIZE_ALL_COLUMNS : JTable.AUTO_RESIZE_OFF);
        prefs.putBoolean(JabRefPreferences.TABLE_PRIMARY_SORT_DESCENDING, priDesc.isSelected());
        prefs.putBoolean(JabRefPreferences.TABLE_SECONDARY_SORT_DESCENDING, secDesc.isSelected());
        prefs.putBoolean(JabRefPreferences.TABLE_TERTIARY_SORT_DESCENDING, terDesc.isSelected());
        prefs.put(JabRefPreferences.TABLE_PRIMARY_SORT_FIELD, priField.getText().toLowerCase().trim());
        prefs.put(JabRefPreferences.TABLE_SECONDARY_SORT_FIELD, secField.getText().toLowerCase().trim());
        prefs.put(JabRefPreferences.TABLE_TERTIARY_SORT_FIELD, terField.getText().toLowerCase().trim());

        prefs.putBoolean(JabRefPreferences.FLOAT_MARKED_ENTRIES, floatMarked.isSelected());
        // updatefont

        String oldVal = prefs.get(JabRefPreferences.NUMERIC_FIELDS);
        String newVal = numericFields.getText().trim();
        if (newVal.isEmpty()) {
            newVal = null;
        }
        if (!Objects.equals(oldVal, newVal)) {
            prefs.put(JabRefPreferences.NUMERIC_FIELDS, newVal);
            InternalBibtexFields.setNumericFieldsFromPrefs();
        }

    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry table");
    }
}
