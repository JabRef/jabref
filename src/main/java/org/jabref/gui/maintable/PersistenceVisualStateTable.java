package org.jabref.gui.maintable;

import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
import javafx.scene.control.TableView;

import org.jabref.gui.maintable.columns.MainTableColumn;

/**
 * Keep track of changes made to the columns (reordering, resorting, resizing).
 */
public class PersistenceVisualStateTable {

    protected final TableView<BibEntryTableViewModel> table;
    protected final ColumnPreferences preferences;

    public PersistenceVisualStateTable(TableView<BibEntryTableViewModel> table, ColumnPreferences preferences) {
        this.table = table;
        this.preferences = preferences;
    }

    public void addListeners() {
        table.getColumns().addListener((InvalidationListener) obs -> updateColumns());
        table.getSortOrder().addListener((InvalidationListener) obs -> updateSortOrder());

        // As we store the ColumnModels of the MainTable, we need to add the listener to the ColumnModel properties,
        // since the value is bound to the model after the listener to the column itself is called.

        table.getColumns().stream()
             .map(col -> ((MainTableColumn<?>) col).getModel())
             .forEach(model -> {
                 model.widthProperty().addListener(obs -> updateColumns());
                 model.sortTypeProperty().addListener(obs -> updateColumns());
             });
    }

    /**
     * Stores shown columns, their width and their sortType in preferences.
     */
    private void updateColumns() {
        preferences.setColumns(
                table.getColumns().stream()
                     .filter(col -> col instanceof MainTableColumn<?>)
                     .map(column -> ((MainTableColumn<?>) column).getModel())
                     .collect(Collectors.toList()));
    }

    /**
     * Stores the SortOrder of the Table in the preferences. Cannot be combined with updateColumns, because JavaFX
     * would provide just an empty list for the sort order on other changes.
     */
    private void updateSortOrder() {
        preferences.setColumnSortOrder(
                table.getSortOrder().stream()
                     .filter(col -> col instanceof MainTableColumn<?>)
                     .map(column -> ((MainTableColumn<?>) column).getModel())
                     .collect(Collectors.toList()));
    }
}
