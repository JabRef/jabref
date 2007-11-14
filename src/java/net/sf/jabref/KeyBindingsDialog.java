/*
 Copyright (C) 2003 Morten O.Alver & Nizar N. Batada

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */

package net.sf.jabref;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

//
class KeyBindingsDialog
    extends JDialog {
  KeystrokeTable table;
  KeystrokeTableModel tableModel;
  //JList list = new JList();
  JTextField keyTF = new JTextField();
  JButton ok, cancel, grabB, defB;
  HashMap<String, String> bindHM, defBinds;
  boolean clickedSave = false;
  int selectedRow = -1;
  boolean getAction() {
    return clickedSave;
  }

  HashMap<String, String> getNewKeyBindings() {
    return bindHM;
  }

  public KeyBindingsDialog(HashMap<String, String> name2binding, HashMap<String, String> defBinds) {
    super();
    this.defBinds = defBinds;
    setTitle(Globals.lang("Key bindings"));
    setModal(true); //this needs to be modal so that client knows when ok or cancel was clicked
    getContentPane().setLayout(new BorderLayout());
    bindHM = name2binding;
    setList();
    //JScrollPane listScroller = new JScrollPane(list);
    JScrollPane listScroller = new JScrollPane(table);
    listScroller.setPreferredSize(new Dimension(250, 400));
    getContentPane().add(listScroller, BorderLayout.CENTER);

    Box buttonBox = new Box(BoxLayout.X_AXIS);
    ok = new JButton(Globals.lang("Ok"));
    cancel = new JButton(Globals.lang("Cancel"));
    grabB = new JButton(Globals.lang("Grab"));
    defB = new JButton(Globals.lang("Default"));
    grabB.addKeyListener(new JBM_CustomKeyBindingsListener());
    /*grabB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        selectedRow = (table.getSelectedRows())[0];
        Util.pr(""+selectedRow);
      }
    });*/
    buttonBox.add(grabB);
    buttonBox.add(defB);
    buttonBox.add(ok);
    buttonBox.add(cancel);

    getContentPane().add(buttonBox, BorderLayout.SOUTH);
    //setTop();
    setButtons();
    keyTF.setEditable(false);

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        clickedSave = false;
        dispose();
      }
    });
  }

  void setTop() {
    Box topBox = new Box(BoxLayout.X_AXIS);

    topBox.add(new JLabel(Globals.lang("Binding") + ":", JLabel.RIGHT));
    topBox.add(keyTF);
    getContentPane().add(topBox, BorderLayout.NORTH);

  }

  //##################################################
  // respond to grabKey and display the key binding
  //##################################################
  public class JBM_CustomKeyBindingsListener
      extends KeyAdapter {
    public void keyPressed(KeyEvent evt) {
      // first check if anything is selected if not the return
      int selRow = table.getSelectedRow();
      if (selRow < 0)
        return;
      //Util.pr("dei"+selectedRow+" "+table.getSelectedRow());
      //Object[] selected = list.getSelectedValues();
      //if (selected.length == 0) {
      //  return;
      //}

      String code = KeyEvent.getKeyText(evt.getKeyCode());
      String mod = KeyEvent.getKeyModifiersText(evt.getModifiers());
      // all key bindings must have a modifier: ctrl alt etc

      if (mod.equals("")) {
        int kc = evt.getKeyCode();
        if ( (kc < KeyEvent.VK_F1) && (kc > KeyEvent.VK_F12) &&
            (kc != KeyEvent.VK_ESCAPE) && (kc != KeyEvent.VK_DELETE)) {
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
        newKey = mod.toLowerCase().replaceAll("\\+"," ") + " " + code;
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

  //##################################################
  // put the corresponding key binding into keyTF
  //##################################################
  class MyListSelectionListener
      implements ListSelectionListener {
    // This method is called each time the user changes the set of selected items
    public void valueChanged(ListSelectionEvent evt) {
      // When the user release the mouse button and completes the selection,
      // getValueIsAdjusting() becomes false
      if (!evt.getValueIsAdjusting()) {
        JList list = (JList) evt.getSource();

        // Get all selected items
        Object[] selected = list.getSelectedValues();

        // Iterate all selected items
        for (int i = 0; i < selected.length; i++) {
          Object sel = selected[i];
          keyTF.setText( bindHM.get(sel));
        }
      }
    }
  }

  

  //setup so that clicking on list will display the current binding
  void setList() {

    Iterator<String> it = bindHM.keySet().iterator();
    String[][] tableData = new String[bindHM.size()][3];
    int i=0;
    while (it.hasNext()) {
      String s = it.next();
      tableData[i][2] = s;
      tableData[i][1] = bindHM.get(s);
      tableData[i][0] = Globals.lang(s);
      i++;
      //listModel.addElement(s + " (" + bindHM.get(s) + ")");
   }
   TreeMap<String, String[]> sorted = new TreeMap<String, String[]>();
   for (i=0; i<tableData.length; i++)
     sorted.put(tableData[i][0], tableData[i]);

    tableModel = new KeystrokeTableModel(sorted);
    table = new KeystrokeTable(tableModel);
    //table.setCellSelectionEnabled(false);
    table.setRowSelectionAllowed(true);
    table.setColumnSelectionAllowed(false);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    //list.setModel(listModel);
    TableColumnModel cm = table.getColumnModel();
    cm.getColumn(0).setPreferredWidth(GUIGlobals.KEYBIND_COL_0);
    cm.getColumn(1).setPreferredWidth(GUIGlobals.KEYBIND_COL_1);
    table.setRowSelectionInterval(0, 0); //select the first entry
  }

  class KeystrokeTable extends JTable {
    public KeystrokeTable(KeystrokeTableModel model) { super(model); }
     public boolean isCellEditable(int row, int col) { return false; }
     public String getOriginalName(int row) { return ((KeystrokeTableModel)getModel()).data[row][2]; }
   }

    class KeystrokeTableModel extends AbstractTableModel {
      String[][] data;
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
      public boolean isCellEditable(int row, int col) { return false; }
      public String getColumnName(int col) {
        return (col==0 ? Globals.lang("Action") : Globals.lang("Shortcut"));
      }
      public int getColumnCount() {
        return 2;
      }

      public int getRowCount() {
        return data.length;
      }
      public Object getValueAt(int rowIndex, int columnIndex) {
        //if (columnIndex == 0)
        return data[rowIndex][columnIndex];
        //else
        //return data[rowIndex][0];
      }
      public void setValueAt(Object o, int row, int col) {
        data[row][col] = (String)o;
      }
    }

  // listners
  void setButtons() {
    ok.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // save all the key bindings
        dispose();
        clickedSave = true;
        // message: key bindings will take into effect next time you start JBM
      }
    });
    cancel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        dispose();
        clickedSave = false;
        //System.exit(-1);//get rid of this
      }
    });
    defB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        /*Object[] selected = list.getSelectedValues();
        if (selected.length == 0) {
          return;
        }
        keyTF.setText(setToDefault( (String) list.getSelectedValue()));*/
      }
    });

  }

  String setToDefault(String name) {
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
