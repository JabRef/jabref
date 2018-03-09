package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ListChangeListener;
import javafx.scene.control.TableColumn;

import org.jabref.preferences.JabRefPreferences;

/**
 * Keep track of changes made to the columns, like reordering or resizing.
 *
 */
public class PersistenceVisualStateTable {

    private final MainTable mainTable;
    private final JabRefPreferences preferences;

    public PersistenceVisualStateTable(final MainTable mainTable, JabRefPreferences preferences) {
        this.mainTable = mainTable;
        this.preferences = preferences;

        mainTable.getColumns().addListener(this::onColumnsChanged);
    }

    private void onColumnsChanged(ListChangeListener.Change<? extends TableColumn<BibEntryTableViewModel, ?>> change) {
        boolean changed = false;
        while (change.next()) {
            changed = true;
        }

        if (changed) {
            updateColumnPreferences();
        }
    }

    /**
     * Store shown columns and their width in preferences.
     */
    private void updateColumnPreferences() {
        List<String> columnNames = new ArrayList<>();
        List<String> columnsWidths = new ArrayList<>();

        for (TableColumn<BibEntryTableViewModel, ?> column : mainTable.getColumns()) {
            if (column instanceof NormalTableColumn) {
                NormalTableColumn normalColumn = (NormalTableColumn) column;

                columnNames.add(normalColumn.getColumnName());
                columnsWidths.add(String.valueOf(normalColumn.getWidth()));
            }
        }

        // Finally, we store the new preferences.
        preferences.putStringList(JabRefPreferences.COLUMN_NAMES, columnNames);
        preferences.putStringList(JabRefPreferences.COLUMN_WIDTHS, columnsWidths);
    }
}
