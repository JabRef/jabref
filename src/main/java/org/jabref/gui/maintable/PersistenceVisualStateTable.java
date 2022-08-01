package org.jabref.gui.maintable;

import java.util.stream.Collectors;

import javafx.scene.control.TableView;

import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.preferences.PreferencesService;

/**
 * Keep track of changes made to the columns (reordering, resorting, resizing).
 */
public class PersistenceVisualStateTable extends AbstractPersistenceVisualStateTable {

    public PersistenceVisualStateTable(final TableView<BibEntryTableViewModel> mainTable, PreferencesService preferences) {
        super(mainTable, preferences);
    }

    /**
     * Stores shown columns, their width and their sortType in preferences.
     */
    @Override
    protected void updateColumns() {
        preferences.storeMainTableColumnPreferences(new ColumnPreferences(
                mainTable.getColumns().stream()
                         .filter(col -> col instanceof MainTableColumn<?>)
                         .map(column -> ((MainTableColumn<?>) column).getModel())
                         .collect(Collectors.toList()),
                preferences.getColumnPreferences().getColumnSortOrder()));
    }

    /**
     * Stores the SortOrder of the the Table in the preferences. Cannot be combined with updateColumns, because JavaFX
     * would provide just an empty list for the sort order on other changes.
     */
    @Override
    protected void updateSortOrder() {
        preferences.storeMainTableColumnPreferences(new ColumnPreferences(
                preferences.getColumnPreferences().getColumns(),
                mainTable.getSortOrder().stream()
                         .filter(col -> col instanceof MainTableColumn<?>)
                         .map(column -> ((MainTableColumn<?>) column).getModel())
                         .collect(Collectors.toList())));
    }
}
