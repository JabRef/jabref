/*  Copyright (C) 2013-2015 JabRef contributors.
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.gui.InternalBibtexFields;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Preference tab for file sorting options.
 */
class FileSortTab extends JPanel implements PrefsTab {

    private final JabRefPreferences prefs;

    private final JRadioButton exportInOriginalOrder;
    private final JRadioButton exportInTableOrder;
    private final JRadioButton exportInSpecifiedOrder;
    private final JCheckBox exportPriDesc;
    private final JCheckBox exportSecDesc;
    private final JCheckBox exportTerDesc;
    private final JTextField exportPriField;
    private final JTextField exportSecField;
    private final JTextField exportTerField;
    private final JComboBox<String> exportPriSort;
    private final JComboBox<String> exportSecSort;
    private final JComboBox<String> exportTerSort;


    public FileSortTab(JabRefPreferences prefs) {
        this.prefs = prefs;
        FormLayout layout = new FormLayout("4dlu, left:pref, 4dlu, fill:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.leadingColumnOffset(1);

        // EXPORT SORT ORDER
        // create Components
        exportInOriginalOrder = new JRadioButton(Localization.lang("Export entries in their original order"));
        exportInTableOrder = new JRadioButton(Localization.lang("Export in current table sort order"));
        exportInSpecifiedOrder = new JRadioButton(Localization.lang("Export entries ordered as specified"));

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(exportInOriginalOrder);
        buttonGroup.add(exportInTableOrder);
        buttonGroup.add(exportInSpecifiedOrder);

        ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                boolean selected = e.getSource() == exportInSpecifiedOrder;
                exportPriSort.setEnabled(selected);
                exportPriField.setEnabled(selected);
                exportPriDesc.setEnabled(selected);
                exportSecSort.setEnabled(selected);
                exportSecField.setEnabled(selected);
                exportSecDesc.setEnabled(selected);
                exportTerSort.setEnabled(selected);
                exportTerField.setEnabled(selected);
                exportTerDesc.setEnabled(selected);
            }
        };
        exportInOriginalOrder.addActionListener(listener);
        exportInTableOrder.addActionListener(listener);
        exportInSpecifiedOrder.addActionListener(listener);

        ArrayList<String> fieldNames = new ArrayList<>(InternalBibtexFields.getAllFieldNames());
        fieldNames.add(BibEntry.KEY_FIELD);
        Collections.sort(fieldNames);
        String[] allPlusKey = fieldNames.toArray(new String[fieldNames.size()]);
        exportPriSort = new JComboBox<>(allPlusKey);
        exportSecSort = new JComboBox<>(allPlusKey);
        exportTerSort = new JComboBox<>(allPlusKey);

        exportPriField = new JTextField(10);
        exportSecField = new JTextField(10);
        exportTerField = new JTextField(10);

        exportPriSort.insertItemAt(Localization.lang("<select>"), 0);
        exportSecSort.insertItemAt(Localization.lang("<select>"), 0);
        exportTerSort.insertItemAt(Localization.lang("<select>"), 0);

        exportPriSort.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (exportPriSort.getSelectedIndex() > 0) {
                    exportPriField.setText(exportPriSort.getSelectedItem().toString());
                    exportPriSort.setSelectedIndex(0);
                }
            }
        });
        exportSecSort.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (exportSecSort.getSelectedIndex() > 0) {
                    exportSecField.setText(exportSecSort.getSelectedItem().toString());
                    exportSecSort.setSelectedIndex(0);
                }
            }
        });
        exportTerSort.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (exportTerSort.getSelectedIndex() > 0) {
                    exportTerField.setText(exportTerSort.getSelectedItem().toString());
                    exportTerSort.setSelectedIndex(0);
                }
            }
        });

        exportPriDesc = new JCheckBox(Localization.lang("Descending"));
        exportSecDesc = new JCheckBox(Localization.lang("Descending"));
        exportTerDesc = new JCheckBox(Localization.lang("Descending"));

        // create GUI
        JLabel lab;

        builder.appendSeparator(Localization.lang("Export sort order"));
        builder.append(exportInOriginalOrder, 1);
        builder.nextLine();
        builder.append(exportInTableOrder, 1);
        builder.nextLine();
        builder.append(exportInSpecifiedOrder, 1);
        builder.nextLine();

        // Create a new panel with its own FormLayout for these items:
        FormLayout layout2 = new FormLayout(
                "right:pref, 8dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, left:pref", "");
        DefaultFormBuilder builder2 = new DefaultFormBuilder(layout2);
        lab = new JLabel(Localization.lang("Primary sort criterion"));
        builder2.append(lab);
        builder2.append(exportPriSort);
        builder2.append(exportPriField);
        builder2.append(exportPriDesc);
        builder2.nextLine();
        lab = new JLabel(Localization.lang("Secondary sort criterion"));
        builder2.append(lab);
        builder2.append(exportSecSort);
        builder2.append(exportSecField);
        builder2.append(exportSecDesc);
        builder2.nextLine();
        lab = new JLabel(Localization.lang("Tertiary sort criterion"));
        builder2.append(lab);
        builder2.append(exportTerSort);
        builder2.append(exportTerField);
        builder2.append(exportTerDesc);

        builder.append(builder2.getPanel());
        builder.nextLine();

        // COMBINE EVERYTHING
        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout());
        add(pan, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        if (prefs.getBoolean(JabRefPreferences.EXPORT_IN_ORIGINAL_ORDER)) {
            exportInOriginalOrder.setSelected(true);
        } else if (prefs.getBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER)) {
            exportInSpecifiedOrder.setSelected(true);
        } else {
            exportInTableOrder.setSelected(true);
        }

        boolean selected = prefs.getBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER);
        exportPriSort.setEnabled(selected);
        exportPriField.setEnabled(selected);
        exportPriDesc.setEnabled(selected);
        exportSecSort.setEnabled(selected);
        exportSecField.setEnabled(selected);
        exportSecDesc.setEnabled(selected);
        exportTerSort.setEnabled(selected);
        exportTerField.setEnabled(selected);
        exportTerDesc.setEnabled(selected);

        exportPriField.setText(prefs.get(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD));
        exportSecField.setText(prefs.get(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD));
        exportTerField.setText(prefs.get(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD));

        exportPriSort.setSelectedIndex(0);
        exportSecSort.setSelectedIndex(0);
        exportTerSort.setSelectedIndex(0);

        exportPriDesc.setSelected(prefs.getBoolean(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING));
        exportSecDesc.setSelected(prefs.getBoolean(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING));
        exportTerDesc.setSelected(prefs.getBoolean(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING));
    }

    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.EXPORT_IN_ORIGINAL_ORDER, exportInOriginalOrder.isSelected());
        prefs.putBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER, exportInSpecifiedOrder.isSelected());

        prefs.putBoolean(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING, exportPriDesc.isSelected());
        prefs.putBoolean(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING, exportSecDesc.isSelected());
        prefs.putBoolean(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING, exportTerDesc.isSelected());

        prefs.put(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD, exportPriField.getText().toLowerCase().trim());
        prefs.put(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD, exportSecField.getText().toLowerCase().trim());
        prefs.put(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD, exportTerField.getText().toLowerCase().trim());

    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Export Sorting");
    }
}
