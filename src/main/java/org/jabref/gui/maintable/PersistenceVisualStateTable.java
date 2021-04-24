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

        mainTable.getColumns().addListener((InvalidationListener) obs -> updateColumns());
        mainTable.getSortOrder().addListener((InvalidationListener) obs -> updateSortOrder());

        // As we store the ColumnModels of the MainTable, we need to add the listener to the ColumnModel properties,
        // since the value is bound to the model after the listener to the column itself is called.
        mainTable.getColumns().forEach(col ->
                ((MainTableColumn<?>) col).getModel().widthProperty().addListener(obs -> updateColumns()));
        mainTable.getColumns().forEach(col ->
                ((MainTableColumn<?>) col).getModel().sortTypeProperty().addListener(obs -> updateColumns()));
    }

    /**
     * Stores shown columns, their width and their sortType in preferences.
     */
    private void updateColumns() {
        preferences.storeColumnPreferences(new ColumnPreferences(
                mainTable.getColumns().stream()
                         .map(column -> ((MainTableColumn<?>) column).getModel())
                         .collect(Collectors.toList()),
                preferences.getColumnPreferences().getColumnSortOrder()));
    }

    /**
     * Stores the SortOrder of the the Table in the preferences. Cannot be combined with updateColumns, because JavaFX
     * would provide just an empty list for the sort order on other changes.
     */
    private void updateSortOrder() {
        preferences.storeColumnPreferences(new ColumnPreferences(
                preferences.getColumnPreferences().getColumns(),
                mainTable.getSortOrder().stream()
                         .map(column -> ((MainTableColumn<?>) column).getModel())
                         .collect(Collectors.toList())));
    }
}
