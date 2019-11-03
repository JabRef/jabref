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
        List<MainTableColumnModel> columns = new ArrayList<>();

        mainTable.getColumns().forEach(column -> {
            MainTableColumnModel columnModel = ((MainTableColumn<?>) column).getModel();
            columnModel.setWidth(column.getWidth());
            columns.add(columnModel);
        });

        ColumnPreferences oldColumnPreferences = preferences.getColumnPreferences();

        preferences.storeColumnPreferences(new ColumnPreferences(
                columns,
                oldColumnPreferences.getSpecialFieldsEnabled(),
                oldColumnPreferences.getAutoSyncSpecialFieldsToKeyWords(),
                oldColumnPreferences.getSerializeSpecialFields(),
                oldColumnPreferences.getExtraFileColumnsEnabled(),
                oldColumnPreferences.getSortTypesForColumns()));
    }
}
