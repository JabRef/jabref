package org.jabref.gui.maintable;

import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;

import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.preferences.PreferencesService;

/**
 * Keep track of changes made to the columns (reordering, resorting, resizing).
 */
public class PersistenceVisualStateTable {

    private final MainTable mainTable;
    private final PreferencesService preferences;

    public PersistenceVisualStateTable(final MainTable mainTable, PreferencesService preferences) {
        this.mainTable = mainTable;
        this.preferences = preferences;

        mainTable.getColumns().addListener((InvalidationListener) obs -> updateColumnPreferences());
        mainTable.getSortOrder().addListener((InvalidationListener) obs -> updateColSortOrder());

        // As we store the ColumnModels of the MainTable, we need to add the listener to the ColumnModel properties,
        // since the value is bound to the model after the listener to the column itself is called.
        mainTable.getColumns().forEach(col -> ((MainTableColumn<?>) col).getModel().widthProperty().addListener(obs -> updateColumnPreferences()));
        mainTable.getColumns().forEach(col -> ((MainTableColumn<?>) col).getModel().sortTypeProperty().addListener(obs -> updateColumnPreferences()));
    }

    private void updateColSortOrder() {
        ColumnPreferences prefs = preferences.getColumnPreferences();

        var sortOrder = mainTable.getSortOrder().stream()
                        .map(column -> ((MainTableColumn<?>) column).getModel())
                        .collect(Collectors.toList());

        prefs.setColumnSorOrder(sortOrder);
        preferences.storeColumnPreferences(prefs);
    }

    /**
     * Store shown columns, their width and their sortType in preferences.
     */
    private void updateColumnPreferences() {

        ColumnPreferences prefs = preferences.getColumnPreferences();
        prefs.setColumns(mainTable.getColumns().stream()
                         .map(column -> ((MainTableColumn<?>) column).getModel())
                         .collect(Collectors.toList()));
        preferences.storeColumnPreferences(prefs);

    }
}
