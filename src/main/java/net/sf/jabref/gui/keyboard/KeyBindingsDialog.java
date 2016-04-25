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
package net.sf.jabref.gui.keyboard;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Dialog to customize key bindings
 */
@SuppressWarnings("serial")
public class KeyBindingsDialog extends JDialog {

    private final JButton ok = new JButton(Localization.lang("OK"));

    private final JButton cancel = new JButton(Localization.lang("Cancel"));
    private final JButton resetToDefaultKeyBindings = new JButton(Localization.lang("Default"));
    private final JButton grabB = new JButton(Localization.lang("Grab"));
    private final Box buttonBox = new Box(BoxLayout.X_AXIS);

    // stores the user-selected key bindings
    private final KeyBindingRepository keyBindingRepository;

    private final KeyBindingTable table;

    public KeyBindingsDialog(KeyBindingRepository keyBindingRepository) {
        super();
        setTitle(Localization.lang("Key bindings"));
        setModal(true); //this needs to be modal so that client knows when ok or cancel was clicked
        getContentPane().setLayout(new BorderLayout());
        this.keyBindingRepository = keyBindingRepository;
        this.table = setupTable();
        updateTableData();
        //JScrollPane listScroller = new JScrollPane(list);
        JScrollPane listScroller = new JScrollPane(table);
        listScroller.setPreferredSize(new Dimension(250, 400));
        getContentPane().add(listScroller, BorderLayout.CENTER);

        grabB.addKeyListener(new KeyBindingsListener(table));
        buttonBox.add(grabB);
        buttonBox.add(resetToDefaultKeyBindings);
        buttonBox.add(ok);
        buttonBox.add(cancel);

        getContentPane().add(buttonBox, BorderLayout.SOUTH);
        //setTop();
        activateListeners();

        KeyBinder.bindCloseDialogKeyToCancelAction(getRootPane(), cancel.getAction());

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }

    private KeyBindingTable setupTable() {
        KeyBindingTable table = new KeyBindingTable();
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        return table;
    }

    private void updateTableData() {
        KeyBindingTableModel tableModel = new KeyBindingTableModel(keyBindingRepository);
        table.setModel(tableModel);

        // has to be done each time as the columnModel is dependent on the tableModel
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(GUIGlobals.KEYBIND_COL_0);
        cm.getColumn(1).setPreferredWidth(GUIGlobals.KEYBIND_COL_1);
    }

    private void activateListeners() {
        ok.addActionListener(e -> {
            // save all the key bindings
            Globals.getKeyPrefs().setNewKeyBindings(keyBindingRepository.getKeyBindings());

            // show message
            JOptionPane.showMessageDialog
                    (KeyBindingsDialog.this,
                            Localization.lang("Your new key bindings have been stored.") + '\n'
                                    + Localization.lang("You must restart JabRef for the new key "
                                    + "bindings to work properly."),
                            Localization.lang("Key bindings changed"),
                            JOptionPane.INFORMATION_MESSAGE);

            dispose();
        });
        cancel.addActionListener(e -> dispose());
        resetToDefaultKeyBindings.addActionListener(e -> {
            int[] selected = table.getSelectedRows();
            boolean hasNothingSelected = selected.length == 0;
            if (hasNothingSelected) {
                int answer = JOptionPane.showOptionDialog(KeyBindingsDialog.this,
                        Localization.lang("All key bindings will be reset to their defaults.") + " " +
                        Localization.lang("Continue?"),
                        Localization.lang("Resetting all key bindings"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null,
                        new String[] {Localization.lang("OK"),
                                Localization.lang("Cancel")},
                        Localization.lang("OK"));
                if (answer == JOptionPane.YES_OPTION) {
                    keyBindingRepository.resetToDefault();
                    updateTableData();
                }
            } else {
                for (int row : selected) {
                    String name = String.valueOf(table.getValueAt(row, 0));
                    keyBindingRepository.resetToDefault(name);
                    String newKey = keyBindingRepository.get(name);
                    table.setValueAt(newKey, row, 1);
                    table.repaint();
                }
            }
        });

    }
}
