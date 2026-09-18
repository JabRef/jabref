package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
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
    private final ListChangeListener<TableColumn<BibEntryTableViewModel, ?>> columnsListener = _ -> {
        synchronizeObservedColumns();
        updateColumns();
    };
    private final ListChangeListener<TableColumn<BibEntryTableViewModel, ?>> sortOrderListener = _ -> updateSortOrder();
    private final Map<MainTableColumnModel, ColumnModelListeners> observedColumnModels = new IdentityHashMap<>();
    private boolean listenersInstalled;
    private int persistenceSuppressionDepth;

    public PersistenceVisualStateTable(TableView<BibEntryTableViewModel> table, ColumnPreferences preferences) {
        this.table = table;
        this.preferences = preferences;
    }

    public void addListeners() {
        if (listenersInstalled) {
            return;
        }

        table.getColumns().addListener(columnsListener);
        table.getSortOrder().addListener(sortOrderListener);
        listenersInstalled = true;

        synchronizeObservedColumns();
    }

    public void dispose() {
        if (!listenersInstalled) {
            return;
        }

        table.getColumns().removeListener(columnsListener);
        table.getSortOrder().removeListener(sortOrderListener);
        listenersInstalled = false;

        new ArrayList<>(observedColumnModels.keySet()).forEach(this::stopObservingColumnModel);
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

    private void synchronizeObservedColumns() {
        Set<MainTableColumnModel> currentModels = table.getColumns().stream()
                                                       .filter(MainTableColumn.class::isInstance)
                                                       .map(column -> ((MainTableColumn<?>) column).getModel())
                                                       .collect(Collectors.toCollection(() -> Collections.newSetFromMap(new IdentityHashMap<>())));

        new ArrayList<>(observedColumnModels.keySet()).stream()
                                                      .filter(model -> !currentModels.contains(model))
                                                      .forEach(this::stopObservingColumnModel);
        currentModels.forEach(this::observeColumnModel);
    }

    private void observeColumnModel(MainTableColumnModel model) {
        if (observedColumnModels.containsKey(model)) {
            return;
        }

        InvalidationListener widthListener = _ -> updateColumns();
        InvalidationListener sortTypeListener = _ -> updateColumns();
        model.widthProperty().addListener(widthListener);
        model.sortTypeProperty().addListener(sortTypeListener);
        observedColumnModels.put(model, new ColumnModelListeners(widthListener, sortTypeListener));
    }

    private void stopObservingColumnModel(MainTableColumnModel model) {
        ColumnModelListeners listeners = observedColumnModels.remove(model);
        if (listeners == null) {
            return;
        }

        model.widthProperty().removeListener(listeners.widthListener());
        model.sortTypeProperty().removeListener(listeners.sortTypeListener());
    }

    private List<MainTableColumnModel> toList(List<TableColumn<BibEntryTableViewModel, ?>> columns) {
        return columns.stream()
                      .filter(col -> col instanceof MainTableColumn<?>)
                      .map(column -> ((MainTableColumn<?>) column).getModel())
                      .filter(MainTableColumnModel::isConfigurable)
                      .collect(Collectors.toList());
    }

    private record ColumnModelListeners(InvalidationListener widthListener,
                                        InvalidationListener sortTypeListener) {
    }
}
