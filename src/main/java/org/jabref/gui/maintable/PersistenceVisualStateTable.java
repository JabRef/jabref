package org.jabref.gui.maintable;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.maintable.columns.MainTableColumn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keep track of changes made to the columns (reordering, resorting, resizing).
 */
public class PersistenceVisualStateTable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceVisualStateTable.class);

    protected final TableView<BibEntryTableViewModel> table;
    protected final ColumnPreferences preferences;

    public PersistenceVisualStateTable(TableView<BibEntryTableViewModel> table, ColumnPreferences preferences) {
        this.table = table;
        this.preferences = preferences;
    }

    public void addListeners() {
        table.getColumns().addListener((InvalidationListener) obs -> updateColumns());
        table.getSortOrder().addListener((ListChangeListener<? super TableColumn<BibEntryTableViewModel, ?>>) obs -> updateSortOrder());

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
     * Stores shown columns, their width and their {@link TableColumn.SortType} in preferences.
     * The conversion to the "real" string in the preferences is made at
     * {@link org.jabref.preferences.JabRefPreferences#getColumnSortTypesAsStringList(ColumnPreferences)}
     */
    private void updateColumns() {
        List<MainTableColumnModel> list = toList(table.getColumns());
        LOGGER.debug("Updating columns to {}", list);
        preferences.setColumns(list);
    }

    /**
     * Stores the SortOrder of the Table in the preferences. This includes {@link TableColumn.SortType}.
     * <br>
     * Cannot be combined with updateColumns, because JavaFX would provide just an empty list for the sort order
     * on other changes.
     */
    private void updateSortOrder() {
        LOGGER.debug("Updating sort order");
        preferences.setColumnSortOrder(toList(table.getSortOrder()));
    }

    private List<MainTableColumnModel> toList(List<TableColumn<BibEntryTableViewModel, ?>> columns) {
        return columns.stream()
                .filter(col -> col instanceof MainTableColumn<?>)
                .map(column -> ((MainTableColumn<?>) column).getModel())
                .collect(Collectors.toList());
    }
}
