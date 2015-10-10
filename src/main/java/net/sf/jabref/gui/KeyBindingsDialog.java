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
package net.sf.jabref.gui;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

/**
 * Dialog to customize key bindings
 */
@SuppressWarnings("serial")
class KeyBindingsDialog extends JDialog {

    private KeystrokeTable table;
    //JList list = new JList();

    // displays the key binding of the currently selected entry
    // currently not displayed as it does not get updated
    private final JTextField keyTF = new JTextField();

    private final JButton ok;
    private final JButton cancel;
    private final JButton defB;

    // stores the user-selected key bindings
    private final HashMap<String, String> bindHM;

    // stores default key bindings
    private final HashMap<String, String> defBinds;

    private boolean clickedSave;


    /**
     * Checked by the caller whether user has confirmed the change
     * @return true if the user wants the keybindings to be stored
     */
    boolean getAction() {
        return clickedSave;
    }

    /**
     * Used by the caller to retrieve the keybindings
     */
    HashMap<String, String> getNewKeyBindings() {
        return bindHM;
    }

    public KeyBindingsDialog(HashMap<String, String> name2binding, HashMap<String, String> defBinds) {
        super();
        this.defBinds = defBinds;
        setTitle(Localization.lang("Key bindings"));
        setModal(true); //this needs to be modal so that client knows when ok or cancel was clicked
        getContentPane().setLayout(new BorderLayout());
        bindHM = name2binding;
        setupTable();
        setList();
        //JScrollPane listScroller = new JScrollPane(list);
        JScrollPane listScroller = new JScrollPane(table);
        listScroller.setPreferredSize(new Dimension(250, 400));
        getContentPane().add(listScroller, BorderLayout.CENTER);

        Box buttonBox = new Box(BoxLayout.X_AXIS);
        ok = new JButton(Localization.lang("Ok"));
        cancel = new JButton(Localization.lang("Cancel"));
        JButton grabB = new JButton(Localization.lang("Grab"));
        defB = new JButton(Localization.lang("Default"));
        grabB.addKeyListener(new JBM_CustomKeyBindingsListener());
        buttonBox.add(grabB);
        buttonBox.add(defB);
        buttonBox.add(ok);
        buttonBox.add(cancel);

        getContentPane().add(buttonBox, BorderLayout.SOUTH);
        //setTop();
        setButtons();
        keyTF.setEditable(false);

        Util.bindCloseDialogKeyToCancelAction(getRootPane(), cancel.getAction());

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                clickedSave = false;
                dispose();
            }
        });
    }

    private void setupTable() {
        table = new KeystrokeTable();
        //table.setCellSelectionEnabled(false);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        // TODO: setup so that clicking on list will display the current binding
    }

    private void setTop() {
        Box topBox = new Box(BoxLayout.X_AXIS);

        topBox.add(new JLabel(Localization.lang("Binding") + ":", JLabel.RIGHT));
        topBox.add(keyTF);
        getContentPane().add(topBox, BorderLayout.NORTH);

    }


    /**
     * respond to grabKey and display the key binding
     */
    private class JBM_CustomKeyBindingsListener
            extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent evt) {
            // first check if anything is selected if not the return
            int selRow = table.getSelectedRow();
            if (selRow < 0) {
                return;
            }

            String code = KeyEvent.getKeyText(evt.getKeyCode());
            String mod = KeyEvent.getKeyModifiersText(evt.getModifiers());
            // all key bindings must have a modifier: ctrl alt etc

            if (mod.equals("")) {
                int kc = evt.getKeyCode();
                if (!(kc >= KeyEvent.VK_F1 && kc <= KeyEvent.VK_F12 ||
                        kc == KeyEvent.VK_ESCAPE || kc == KeyEvent.VK_DELETE)) {
                    return; // need a modifier except for function keys
                }
            }
            // second key cannot be a modifiers
            //if ( evt.isActionKey()) {
            //Util.pr(code);
            if ( //code.equals("Escape")
            code.equals("Tab")
                    || code.equals("Backspace")
                    || code.equals("Enter")
                    //|| code.equals("Delete")
                    || code.equals("Space")
                    || code.equals("Ctrl")
                    || code.equals("Shift")
                    || code.equals("Alt")) {
                return;
            }
            //}
            String newKey;
            if (!mod.equals("")) {
                newKey = mod.toLowerCase().replaceAll("\\+", " ") + " " + code;
            }
            else {
                newKey = code;
            }
            keyTF.setText(newKey);
            //find which key is selected and set its value int the bindHM
            String selectedFunction = table.getOriginalName(selRow);
            table.setValueAt(newKey, selRow, 1);
            table.revalidate();
            table.repaint();
            //Util.pr(selectedFunction);
            //String selectedFunction = (String) list.getSelectedValue();
            // log print
            // System.out.println("selectedfunction " + selectedFunction + " new key: " + newKey);
            bindHM.put(selectedFunction, newKey);
            //table.setValueAt(newKey, );
        }
    }

    /**
     * put the corresponding key binding into keyTF
     */
    private class MyListSelectionListener
            implements ListSelectionListener {

        // This method is called each time the user changes the set of selected items
        @Override
        public void valueChanged(ListSelectionEvent evt) {
            // When the user release the mouse button and completes the selection,
            // getValueIsAdjusting() becomes false
            if (!evt.getValueIsAdjusting()) {
                JList list = (JList) evt.getSource();

                // Get all selected items
                Object[] selected = list.getSelectedValues();

                // Iterate all selected items
                for (Object sel : selected) {
                    keyTF.setText(bindHM.get(sel));
                }
            }
        }
    }


    /**
     * Puts the content of bindHM into the table
     */
    private void setList() {
        Iterator<String> it = bindHM.keySet().iterator();
        String[][] tableData = new String[bindHM.size()][3];
        int i = 0;
        while (it.hasNext()) {
            String s = it.next();
            tableData[i][2] = s;
            tableData[i][1] = bindHM.get(s);
            tableData[i][0] = Localization.lang(s);
            i++;
            //listModel.addElement(s + " (" + bindHM.get(s) + ")");
        }
        TreeMap<String, String[]> sorted = new TreeMap<String, String[]>();
        for (i = 0; i < tableData.length; i++) {
            sorted.put(tableData[i][0], tableData[i]);
        }

        KeystrokeTableModel tableModel = new KeystrokeTableModel(sorted);
        table.setModel(tableModel);

        // has to be done each time as the columnModel is dependend on the tableModel
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(GUIGlobals.KEYBIND_COL_0);
        cm.getColumn(1).setPreferredWidth(GUIGlobals.KEYBIND_COL_1);
        //    table.setRowSelectionInterval(0, 0); //select the first entry

    }


    @SuppressWarnings("serial")
    private class KeystrokeTable extends JTable {

        public KeystrokeTable() {
            super();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public String getOriginalName(int row) {
            return ((KeystrokeTableModel) getModel()).data[row][2];
        }
    }

    @SuppressWarnings("serial")
    private class KeystrokeTableModel extends AbstractTableModel {

        final String[][] data;


        //String[] trData;
        public KeystrokeTableModel(TreeMap<String, String[]> sorted) {
            data = new String[sorted.size()][3];
            Iterator<String> i = sorted.keySet().iterator();
            int row = 0;
            while (i.hasNext()) {
                data[row++] = sorted.get(i.next());
            }
            //for (int i=0; i<trData.length; i++)
            //  trData[i] = Globals.lang(data[i][0]);
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        @Override
        public String getColumnName(int col) {
            return col == 0 ? Localization.lang("Action") : Localization.lang("Shortcut");
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            //if (columnIndex == 0)
            return data[rowIndex][columnIndex];
            //else
            //return data[rowIndex][0];
        }

        @Override
        public void setValueAt(Object o, int row, int col) {
            data[row][col] = (String) o;
        }
    }


    // listners
    private void setButtons() {
        ok.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // save all the key bindings
                dispose();
                clickedSave = true;
                // also displays message: key bindings will take into effect next time you start JBM
            }
        });
        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                clickedSave = false;
            }
        });
        defB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selected = table.getSelectedRows();
                if (selected.length == 0) {
                    int answer = JOptionPane.showOptionDialog(KeyBindingsDialog.this,
                            Localization.lang("All key bindings will be reset to their defaults.") + " " + 
                            Localization.lang("Continue?"),
                            Localization.lang("Resetting all key bindings"),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE, null,
                            new String[] {Localization.lang("Ok"), Localization.lang("Cancel")},
                            Localization.lang("Ok"));
                    if (answer == JOptionPane.YES_OPTION) {
                        bindHM.clear();
                        Set<Entry<String, String>> entrySet = defBinds.entrySet();
                        for (Entry<String, String> entry : entrySet) {
                            bindHM.put(entry.getKey(), entry.getValue());
                        }
                        setList();
                    }
                } else {
                    for (int row : selected) {
                        String name = (String) table.getValueAt(row, 0);
                        String newKey = setToDefault(name);
                        keyTF.setText(newKey);
                        table.setValueAt(newKey, row, 1);
                        table.repaint();
                    }
                }
            }
        });

    }

    /**
     * Resets a single accelerator key
     * @param name the action name
     * @return the default accelerator key
     */
    private String setToDefault(String name) {
        String defKey = defBinds.get(name);
        bindHM.put(name, defKey);
        return defKey;
    }

    /*
         public static void main(String args[])
         {
      HashMap h=new HashMap();
      h.put("new-bibtex","ctrl N");
      h.put("edit-bibtex","ctrl E");
      h.put("exit-bibtex","ctrl Q");
      KeyBindingsDialog d= new KeyBindingsDialog(h);
      d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      d.setSize(200,300);
      d.setVisible(true);

      }*/
}
