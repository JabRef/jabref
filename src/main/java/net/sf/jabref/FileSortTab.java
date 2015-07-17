/*  Copyright (C) 2013 JabRef contributors.
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
import java.util.ArrayList;
import java.util.Arrays;
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

/**
 * Preference tab for file sorting options.
 */
@SuppressWarnings("serial")
public class FileSortTab extends JPanel implements PrefsTab {

    private final JabRefPreferences _prefs;
    private final JRadioButton saveInOriginalOrder;
    private final JRadioButton saveInTableOrder;
    private final JRadioButton saveInSpecifiedOrder;
    private JCheckBox savePriDesc, saveSecDesc, saveTerDesc;
    private JTextField savePriField, saveSecField, saveTerField;
    private JComboBox savePriSort, saveSecSort, saveTerSort;

    private final JRadioButton exportInOriginalOrder;
    private final JRadioButton exportInTableOrder;
    private final JRadioButton exportInSpecifiedOrder;
    private JCheckBox exportPriDesc, exportSecDesc, exportTerDesc;
    private JTextField exportPriField, exportSecField, exportTerField;
    private JComboBox exportPriSort, exportSecSort, exportTerSort;


    public FileSortTab(JabRefFrame frame, JabRefPreferences prefs) {
        this._prefs = prefs;
        FormLayout layout = new FormLayout("4dlu, left:pref, 4dlu, fill:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.leadingColumnOffset(1);

        { // SAVE SORT ORDER
          // create Components
            saveInOriginalOrder = new JRadioButton(Globals.lang("Save entries in their original order"));
            saveInTableOrder = new JRadioButton(Globals.lang("Save in current table sort order"));
            saveInSpecifiedOrder = new JRadioButton(Globals.lang("Save entries ordered as specified"));

            ButtonGroup bg = new ButtonGroup();
            bg.add(saveInOriginalOrder);
            bg.add(saveInTableOrder);
            bg.add(saveInSpecifiedOrder);

            ActionListener listener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean selected = e.getSource() == saveInSpecifiedOrder;
                    savePriSort.setEnabled(selected);
                    savePriField.setEnabled(selected);
                    savePriDesc.setEnabled(selected);
                    saveSecSort.setEnabled(selected);
                    saveSecField.setEnabled(selected);
                    saveSecDesc.setEnabled(selected);
                    saveTerSort.setEnabled(selected);
                    saveTerField.setEnabled(selected);
                    saveTerDesc.setEnabled(selected);
                }
            };
            saveInOriginalOrder.addActionListener(listener);
            saveInTableOrder.addActionListener(listener);
            saveInSpecifiedOrder.addActionListener(listener);

            ArrayList<String> v = new ArrayList<String>(Arrays.asList(BibtexFields.getAllFieldNames()));
            v.add(BibtexFields.KEY_FIELD);
            Collections.sort(v);
            String[] allPlusKey = v.toArray(new String[v.size()]);
            savePriSort = new JComboBox(allPlusKey);
            saveSecSort = new JComboBox(allPlusKey);
            saveTerSort = new JComboBox(allPlusKey);

            savePriField = new JTextField(10);
            saveSecField = new JTextField(10);
            saveTerField = new JTextField(10);

            savePriSort.insertItemAt(Globals.lang("<select>"), 0);
            saveSecSort.insertItemAt(Globals.lang("<select>"), 0);
            saveTerSort.insertItemAt(Globals.lang("<select>"), 0);

            savePriSort.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (savePriSort.getSelectedIndex() > 0) {
                        savePriField.setText(savePriSort.getSelectedItem().toString());
                        savePriSort.setSelectedIndex(0);
                    }
                }
            });
            saveSecSort.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (saveSecSort.getSelectedIndex() > 0) {
                        saveSecField.setText(saveSecSort.getSelectedItem().toString());
                        saveSecSort.setSelectedIndex(0);
                    }
                }
            });
            saveTerSort.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (saveTerSort.getSelectedIndex() > 0) {
                        saveTerField.setText(saveTerSort.getSelectedItem().toString());
                        saveTerSort.setSelectedIndex(0);
                    }
                }
            });

            savePriDesc = new JCheckBox(Globals.lang("Descending"));
            saveSecDesc = new JCheckBox(Globals.lang("Descending"));
            saveTerDesc = new JCheckBox(Globals.lang("Descending"));

            // create GUI
            JLabel lab;

            builder.appendSeparator(Globals.lang("Save sort order"));
            builder.append(saveInOriginalOrder, 1);
            builder.nextLine();
            builder.append(saveInTableOrder, 1);
            builder.nextLine();
            builder.append(saveInSpecifiedOrder, 1);
            builder.nextLine();

            // Create a new panel with its own FormLayout for these items:
            FormLayout layout2 = new FormLayout(
                    "right:pref, 8dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, left:pref", "");
            DefaultFormBuilder builder2 = new DefaultFormBuilder(layout2);
            lab = new JLabel(Globals.lang("Primary sort criterion"));
            builder2.append(lab);
            builder2.append(savePriSort);
            builder2.append(savePriField);
            builder2.append(savePriDesc);
            builder2.nextLine();
            lab = new JLabel(Globals.lang("Secondary sort criterion"));
            builder2.append(lab);
            builder2.append(saveSecSort);
            builder2.append(saveSecField);
            builder2.append(saveSecDesc);
            builder2.nextLine();
            lab = new JLabel(Globals.lang("Tertiary sort criterion"));
            builder2.append(lab);
            builder2.append(saveTerSort);
            builder2.append(saveTerField);
            builder2.append(saveTerDesc);

            JPanel saveSpecPanel = builder2.getPanel();
            builder.append(saveSpecPanel);
            builder.nextLine();
        }

        { // EXPORT SORT ORDER
          // create Components
            exportInOriginalOrder = new JRadioButton(Globals.lang("Export entries in their original order"));
            exportInTableOrder = new JRadioButton(Globals.lang("Export in current table sort order"));
            exportInSpecifiedOrder = new JRadioButton(Globals.lang("Export entries ordered as specified"));

            ButtonGroup bg = new ButtonGroup();
            bg.add(exportInOriginalOrder);
            bg.add(exportInTableOrder);
            bg.add(exportInSpecifiedOrder);

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

            ArrayList<String> v = new ArrayList<String>(Arrays.asList(BibtexFields.getAllFieldNames()));
            v.add(BibtexFields.KEY_FIELD);
            Collections.sort(v);
            String[] allPlusKey = v.toArray(new String[v.size()]);
            exportPriSort = new JComboBox(allPlusKey);
            exportSecSort = new JComboBox(allPlusKey);
            exportTerSort = new JComboBox(allPlusKey);

            exportPriField = new JTextField(10);
            exportSecField = new JTextField(10);
            exportTerField = new JTextField(10);

            exportPriSort.insertItemAt(Globals.lang("<select>"), 0);
            exportSecSort.insertItemAt(Globals.lang("<select>"), 0);
            exportTerSort.insertItemAt(Globals.lang("<select>"), 0);

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

            exportPriDesc = new JCheckBox(Globals.lang("Descending"));
            exportSecDesc = new JCheckBox(Globals.lang("Descending"));
            exportTerDesc = new JCheckBox(Globals.lang("Descending"));

            // create GUI
            JLabel lab;

            builder.appendSeparator(Globals.lang("Export sort order"));
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
            lab = new JLabel(Globals.lang("Primary sort criterion"));
            builder2.append(lab);
            builder2.append(exportPriSort);
            builder2.append(exportPriField);
            builder2.append(exportPriDesc);
            builder2.nextLine();
            lab = new JLabel(Globals.lang("Secondary sort criterion"));
            builder2.append(lab);
            builder2.append(exportSecSort);
            builder2.append(exportSecField);
            builder2.append(exportSecDesc);
            builder2.nextLine();
            lab = new JLabel(Globals.lang("Tertiary sort criterion"));
            builder2.append(lab);
            builder2.append(exportTerSort);
            builder2.append(exportTerField);
            builder2.append(exportTerDesc);

            builder.append(builder2.getPanel());
            builder.nextLine();
        }

        // COMBINE EVERYTHING
        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout());
        add(pan, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        if (_prefs.getBoolean(JabRefPreferences.SAVE_IN_ORIGINAL_ORDER)) {
            saveInOriginalOrder.setSelected(true);
        } else if (_prefs.getBoolean(JabRefPreferences.SAVE_IN_SPECIFIED_ORDER)) {
            saveInSpecifiedOrder.setSelected(true);
        } else {
            saveInTableOrder.setSelected(true);
        }

        {
            boolean selected = _prefs.getBoolean(JabRefPreferences.SAVE_IN_SPECIFIED_ORDER);
            savePriSort.setEnabled(selected);
            savePriField.setEnabled(selected);
            savePriDesc.setEnabled(selected);
            saveSecSort.setEnabled(selected);
            saveSecField.setEnabled(selected);
            saveSecDesc.setEnabled(selected);
            saveTerSort.setEnabled(selected);
            saveTerField.setEnabled(selected);
            saveTerDesc.setEnabled(selected);
        }

        savePriField.setText(_prefs.get(JabRefPreferences.SAVE_PRIMARY_SORT_FIELD));
        saveSecField.setText(_prefs.get(JabRefPreferences.SAVE_SECONDARY_SORT_FIELD));
        saveTerField.setText(_prefs.get(JabRefPreferences.SAVE_TERTIARY_SORT_FIELD));

        savePriSort.setSelectedIndex(0);
        saveSecSort.setSelectedIndex(0);
        saveTerSort.setSelectedIndex(0);

        savePriDesc.setSelected(_prefs.getBoolean(JabRefPreferences.SAVE_PRIMARY_SORT_DESCENDING));
        saveSecDesc.setSelected(_prefs.getBoolean(JabRefPreferences.SAVE_SECONDARY_SORT_DESCENDING));
        saveTerDesc.setSelected(_prefs.getBoolean(JabRefPreferences.SAVE_TERTIARY_SORT_DESCENDING));

        if (_prefs.getBoolean(JabRefPreferences.EXPORT_IN_ORIGINAL_ORDER)) {
            exportInOriginalOrder.setSelected(true);
        } else if (_prefs.getBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER)) {
            exportInSpecifiedOrder.setSelected(true);
        } else {
            exportInTableOrder.setSelected(true);
        }

        {
            boolean selected = _prefs.getBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER);
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

        exportPriField.setText(_prefs.get(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD));
        exportSecField.setText(_prefs.get(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD));
        exportTerField.setText(_prefs.get(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD));

        exportPriSort.setSelectedIndex(0);
        exportSecSort.setSelectedIndex(0);
        exportTerSort.setSelectedIndex(0);

        exportPriDesc.setSelected(_prefs.getBoolean(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING));
        exportSecDesc.setSelected(_prefs.getBoolean(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING));
        exportTerDesc.setSelected(_prefs.getBoolean(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING));
    }

    @Override
    public void storeSettings() {
        _prefs.putBoolean(JabRefPreferences.SAVE_IN_ORIGINAL_ORDER, saveInOriginalOrder.isSelected());
        _prefs.putBoolean(JabRefPreferences.SAVE_IN_SPECIFIED_ORDER, saveInSpecifiedOrder.isSelected());

        _prefs.putBoolean(JabRefPreferences.SAVE_PRIMARY_SORT_DESCENDING, savePriDesc.isSelected());
        _prefs.putBoolean(JabRefPreferences.SAVE_SECONDARY_SORT_DESCENDING, saveSecDesc.isSelected());
        _prefs.putBoolean(JabRefPreferences.SAVE_TERTIARY_SORT_DESCENDING, saveTerDesc.isSelected());

        _prefs.put(JabRefPreferences.SAVE_PRIMARY_SORT_FIELD, savePriField.getText().toLowerCase().trim());
        _prefs.put(JabRefPreferences.SAVE_SECONDARY_SORT_FIELD, saveSecField.getText().toLowerCase().trim());
        _prefs.put(JabRefPreferences.SAVE_TERTIARY_SORT_FIELD, saveTerField.getText().toLowerCase().trim());

        _prefs.putBoolean(JabRefPreferences.EXPORT_IN_ORIGINAL_ORDER, exportInOriginalOrder.isSelected());
        _prefs.putBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER, exportInSpecifiedOrder.isSelected());

        _prefs.putBoolean(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING, exportPriDesc.isSelected());
        _prefs.putBoolean(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING, exportSecDesc.isSelected());
        _prefs.putBoolean(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING, exportTerDesc.isSelected());

        _prefs.put(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD, exportPriField.getText().toLowerCase().trim());
        _prefs.put(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD, exportSecField.getText().toLowerCase().trim());
        _prefs.put(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD, exportTerField.getText().toLowerCase().trim());

    }

    @Override
    public boolean readyToClose() {
        return true;
    }

    @Override
    public String getTabName() {
        return Globals.lang("File Sorting");
    }
}
