package org.jabref.gui.maintable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
        mainTable.getColumns().forEach(col -> {
            MainTableColumn column = (MainTableColumn) col;
            col.sortTypeProperty().addListener(obs -> updateColumnSortType(column.getModel().getName(), column.getSortType()));
        });
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
        ColumnPreferences oldColumnPreferences = preferences.getColumnPreferences();
        preferences.storeColumnPreferences(new ColumnPreferences(
                mainTable.getColumns().stream().map(column -> ((MainTableColumn) column).getModel()).collect(Collectors.toList()),
                oldColumnPreferences.getExtraFileColumnsEnabled(),
                columnsSortOrder));
    }
}
