package net.sf.jabref.gui.keyboard;

import javax.swing.JTable;

@SuppressWarnings("serial")
public class KeyBindingTable extends JTable {

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public String getOriginalName(int row) {
        return ((KeyBindingTableModel) getModel()).getOriginalName(row);
    }

}
