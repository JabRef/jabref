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

/// Keep track of changes made to the columns (reordering, resorting, resizing).
///
/// This is the table-to-preferences direction. The opposite direction is [ColumnPreferencesApplier].
///
/// Resizing and changing the sort type need no listener here: the preferences receive the column models of the
/// table itself, and [ColumnPreferences] reports changes of their properties.
public class ColumnPreferencesRecorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColumnPreferencesRecorder.class);

    protected final TableView<BibEntryTableViewModel> table;
    protected final ColumnPreferences preferences;

    private final InvalidationListener columnsListener = _ -> updateColumns();
    private final ListChangeListener<TableColumn<BibEntryTableViewModel, ?>> sortOrderListener = _ -> updateSortOrder();

    public ColumnPreferencesRecorder(TableView<BibEntryTableViewModel> table, ColumnPreferences preferences) {
        this.table = table;
        this.preferences = preferences;
    }

    public void bind() {
        table.getColumns().addListener(columnsListener);
        table.getSortOrder().addListener(sortOrderListener);
    }

    public void unbind() {
        table.getColumns().removeListener(columnsListener);
        table.getSortOrder().removeListener(sortOrderListener);
    }

    /// Returns the models of the given columns as they are stored in the preferences.
    ///
    /// [ColumnPreferencesApplier] compares with the same projection. If both directions used different ones, a change
    /// written by one direction would be reverted by the other.
    static List<MainTableColumnModel> toPersistedModels(List<? extends TableColumn<BibEntryTableViewModel, ?>> columns) {
        return columns.stream()
                      .filter(col -> col instanceof MainTableColumn<?>)
                      .map(column -> ((MainTableColumn<?>) column).getModel())
                      .filter(MainTableColumnModel::isConfigurable)
                      .collect(Collectors.toList());
    }

    /// Stores shown columns, their width and their [TableColumn.SortType] in preferences.
    /// The conversion to the "real" string in the preferences is made at
    /// [org.jabref.logic.preferences.JabRefCliPreferences#getColumnSortTypesAsStringList(ColumnPreferences)]
    private void updateColumns() {
        List<MainTableColumnModel> list = toPersistedModels(table.getColumns());
        LOGGER.debug("Updating columns to {}", list);
        preferences.setColumns(list);
    }

    /// Stores the SortOrder of the Table in the preferences. This includes [TableColumn.SortType].
    ///
    /// Cannot be combined with updateColumns, because JavaFX would provide just an empty list for the sort order
    /// on other changes.
    private void updateSortOrder() {
        LOGGER.debug("Updating sort order");
        preferences.setColumnSortOrder(toPersistedModels(table.getSortOrder()));
    }
}
