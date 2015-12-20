package net.sf.jabref.gui.keyboard;

import net.sf.jabref.logic.l10n.Localization;

import javax.swing.table.AbstractTableModel;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class KeyBindingTableModel extends AbstractTableModel {

    private final KeyBindingRepository keyBindingRepository;

    public KeyBindingTableModel(KeyBindingRepository keyBindingRepository) {
        this.keyBindingRepository = keyBindingRepository;
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
        return keyBindingRepository.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Map.Entry<KeyBinding, String> row = getRowData(rowIndex);
        if(columnIndex == 0) {
            return row.getKey().getLocalization();
        } else {
            return row.getValue();
        }
    }

    public String getOriginalName(int rowIndex) {
        return getRowData(rowIndex).getKey().getKey();
    }

    private Map.Entry<KeyBinding, String> getRowData(int rowIndex) {
        List<Map.Entry<KeyBinding, String>> entries = new LinkedList<>(keyBindingRepository.getKeyBindings().entrySet());
        return entries.get(rowIndex);
    }

    @Override
    public void setValueAt(Object o, int row, int col) {
        if(col == 1) {
            keyBindingRepository.put(getRowData(row).getKey(), String.valueOf(o));
        }
    }
}
