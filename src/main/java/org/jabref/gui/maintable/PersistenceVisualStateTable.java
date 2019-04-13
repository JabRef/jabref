package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.collections.ListChangeListener;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;

import org.jabref.preferences.JabRefPreferences;

/**
 * Keep track of changes made to the columns, like reordering or resizing.
 *
 */
public class PersistenceVisualStateTable {

    private final MainTable mainTable;
    private final JabRefPreferences preferences;
    private final Map<String, SortType> columnsSortOrder = new LinkedHashMap<>();

    public PersistenceVisualStateTable(final MainTable mainTable, JabRefPreferences preferences) {
        this.mainTable = mainTable;
        this.preferences = preferences;

        mainTable.getColumns().addListener(this::onColumnsChanged);
        mainTable.getColumns().forEach(col -> col.sortTypeProperty().addListener(obs ->
                updateColumnSortType(col.getText(), col.getSortType())));
        mainTable.getColumns().forEach(col -> col.widthProperty().addListener(obs -> updateColumnPreferences()));

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

    private void updateColumnSortType(String text, SortType sortType) {
        columnsSortOrder.put(text, sortType);
        preferences.setMainTableColumnSortType(columnsSortOrder);
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

        if (columnNames.size() == columnsWidths.size() &&
                columnNames.size() == preferences.getStringList(JabRefPreferences.COLUMN_NAMES).size()) {
            preferences.putStringList(JabRefPreferences.COLUMN_NAMES, columnNames);
            preferences.putStringList(JabRefPreferences.COLUMN_WIDTHS, columnsWidths);
        }
    }
}
