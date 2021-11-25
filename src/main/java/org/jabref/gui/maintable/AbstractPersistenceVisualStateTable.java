package org.jabref.gui.maintable;

import javafx.beans.InvalidationListener;
import javafx.scene.control.TableView;

import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.preferences.PreferencesService;

/**
 * Keep track of changes made to the columns (reordering, resorting, resizing).
 */
public abstract class AbstractPersistenceVisualStateTable {

    protected final TableView<BibEntryTableViewModel> mainTable;
    protected final PreferencesService preferences;

    public AbstractPersistenceVisualStateTable(final TableView<BibEntryTableViewModel> mainTable, PreferencesService preferences) {
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
     * override in subclass
     */
    protected abstract void updateColumns();

    /**
     * Stores the SortOrder of the table in the preferences. Cannot be combined with updateColumns, because JavaFX
     * would provide just an empty list for the sort order on other changes.
     * override in subclass
     */
    protected abstract void updateSortOrder();
}
