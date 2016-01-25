package net.sf.jabref.gui.keyboard;

import javax.swing.*;

@SuppressWarnings("serial")
public class KeyBindingTable extends JTable {

    public KeyBindingTable() {
        super();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public String getOriginalName(int row) {
        return ((KeyBindingTableModel) getModel()).getOriginalName(row);
    }

}
