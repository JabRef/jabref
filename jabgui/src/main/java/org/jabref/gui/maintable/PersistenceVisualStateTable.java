package org.jabref.gui.maintable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.collections.ListChangeListener;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.maintable.columns.MainTableColumn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Keep track of changes made to the columns (reordering, resorting, resizing).
public class PersistenceVisualStateTable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceVisualStateTable.class);

    protected final TableView<BibEntryTableViewModel> table;
    protected final ColumnPreferences preferences;
    private final Set<MainTableColumnModel> observedColumnModels = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private int persistenceSuppressionDepth;

    public PersistenceVisualStateTable(TableView<BibEntryTableViewModel> table, ColumnPreferences preferences) {
        this.table = table;
        this.preferences = preferences;
    }

    public void addListeners() {
        table.getColumns().addListener((ListChangeListener<? super TableColumn<BibEntryTableViewModel, ?>>) _ -> {
            observeCurrentColumns();
            updateColumns();
        });
        table.getSortOrder().addListener((ListChangeListener<? super TableColumn<BibEntryTableViewModel, ?>>) _ -> updateSortOrder());

        observeCurrentColumns();
    }

    public void runWithoutPersisting(Runnable operation) {
        persistenceSuppressionDepth++;
        try {
            operation.run();
        } finally {
            persistenceSuppressionDepth--;
        }
    }

    /// Stores shown columns, their width and their [TableColumn.SortType] in preferences.
    /// The conversion to the "real" string in the preferences is made at
    /// [org.jabref.logic.preferences.JabRefCliPreferences#getColumnSortTypesAsStringList(ColumnPreferences)]
    private void updateColumns() {
        if (persistenceSuppressionDepth > 0) {
            return;
        }

        List<MainTableColumnModel> list = toList(table.getColumns());
        LOGGER.debug("Updating columns to {}", list);
        preferences.setColumns(list);
    }

    /// Stores the SortOrder of the Table in the preferences. This includes [TableColumn.SortType].
    ///
    /// Cannot be combined with updateColumns, because JavaFX would provide just an empty list for the sort order
    /// on other changes.
    private void updateSortOrder() {
        if (persistenceSuppressionDepth > 0) {
            return;
        }

        LOGGER.debug("Updating sort order");
        preferences.setColumnSortOrder(toList(table.getSortOrder()));
    }

    private void observeCurrentColumns() {
        // As we store the ColumnModels of the MainTable, we need to add the listener to the ColumnModel properties,
        // since the value is bound to the model after the listener to the column itself is called.
        table.getColumns().stream()
             .filter(MainTableColumn.class::isInstance)
             .map(column -> ((MainTableColumn<?>) column).getModel())
             .forEach(this::observeColumnModel);
    }

    private void observeColumnModel(MainTableColumnModel model) {
        if (!observedColumnModels.add(model)) {
            return;
        }

        model.widthProperty().addListener(_ -> updateColumns());
        model.sortTypeProperty().addListener(_ -> updateColumns());
    }

    private List<MainTableColumnModel> toList(List<TableColumn<BibEntryTableViewModel, ?>> columns) {
        return columns.stream()
                      .filter(col -> col instanceof MainTableColumn<?>)
                      .map(column -> ((MainTableColumn<?>) column).getModel())
                      .filter(model -> model.getType() != MainTableColumnModel.Type.MATCH_CATEGORY)
                      .collect(Collectors.toList());
    }
}
