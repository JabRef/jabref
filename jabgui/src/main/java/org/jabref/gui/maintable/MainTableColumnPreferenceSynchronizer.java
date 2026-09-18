package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.maintable.columns.LibraryColumn;
import org.jabref.gui.maintable.columns.MainTableColumn;

import org.jspecify.annotations.NullMarked;

@NullMarked
class MainTableColumnPreferenceSynchronizer {

    private final TableView<BibEntryTableViewModel> table;
    private final MainTableColumnFactory columnFactory;
    private final MainTablePreferences mainTablePreferences;
    private final PersistenceVisualStateTable persistenceVisualStateTable;
    private final ListChangeListener<MainTableColumnModel> columnsListener = _ -> synchronizeConfiguredColumns();
    private final ListChangeListener<MainTableColumnModel> sortOrderListener = _ -> synchronizeSortOrder();
    private final ChangeListener<Boolean> resizeColumnsListener = (_, _, newValue) -> updateColumnResizePolicy(newValue);

    private boolean listenersInstalled;

    MainTableColumnPreferenceSynchronizer(TableView<BibEntryTableViewModel> table,
                                          MainTableColumnFactory columnFactory,
                                          MainTablePreferences mainTablePreferences,
                                          PersistenceVisualStateTable persistenceVisualStateTable) {
        this.table = table;
        this.columnFactory = columnFactory;
        this.mainTablePreferences = mainTablePreferences;
        this.persistenceVisualStateTable = persistenceVisualStateTable;
    }

    void initializeColumns() {
        table.getColumns().setAll(columnFactory.createColumns());
        table.getColumns().removeIf(LibraryColumn.class::isInstance);
        updateColumnResizePolicy(mainTablePreferences.getResizeColumnsToFit());
    }

    void addListeners() {
        if (listenersInstalled) {
            return;
        }

        mainTablePreferences.getColumnPreferences().getColumns().addListener(columnsListener);
        mainTablePreferences.getColumnPreferences().getColumnSortOrder().addListener(sortOrderListener);
        mainTablePreferences.resizeColumnsToFitProperty().addListener(resizeColumnsListener);
        listenersInstalled = true;
    }

    void dispose() {
        if (!listenersInstalled) {
            return;
        }

        mainTablePreferences.getColumnPreferences().getColumns().removeListener(columnsListener);
        mainTablePreferences.getColumnPreferences().getColumnSortOrder().removeListener(sortOrderListener);
        mainTablePreferences.resizeColumnsToFitProperty().removeListener(resizeColumnsListener);
        listenersInstalled = false;
    }

    void restoreConfiguredSortOrder() {
        List<TableColumn<BibEntryTableViewModel, ?>> restoredSortOrder =
                new ArrayList<>(getConfiguredSortOrder()
                        .stream()
                        .map(this::findColumn)
                        .flatMap(Optional::stream)
                        .toList());

        if (!table.getColumns().isEmpty()) {
            restoredSortOrder.addFirst(table.getColumns().getFirst());
        }
        table.getSortOrder().setAll(restoredSortOrder);
    }

    private void synchronizeConfiguredColumns() {
        if (matchesConfiguredColumns()) {
            return;
        }

        persistenceVisualStateTable.runWithoutPersisting(() -> {
            applyConfiguredColumns();
            restoreConfiguredSortOrder();
        });
    }

    private void synchronizeSortOrder() {
        if (matchesConfiguredSortOrder()) {
            return;
        }

        persistenceVisualStateTable.runWithoutPersisting(this::restoreConfiguredSortOrder);
    }

    private void applyConfiguredColumns() {
        List<MainTableColumnModel> preferredColumns = getConfiguredColumns();

        table.getColumns().removeIf(column -> (column instanceof MainTableColumn<?> mainTableColumn)
                && mainTableColumn.getModel().getType() != MainTableColumnModel.Type.MATCH_CATEGORY
                && preferredColumns.stream().noneMatch(preferredColumn -> preferredColumn.equals(mainTableColumn.getModel())));

        for (int i = 0; i < preferredColumns.size(); i++) {
            MainTableColumnModel preferredColumn = preferredColumns.get(i);
            TableColumn<BibEntryTableViewModel, ?> tableColumn = findColumn(preferredColumn)
                    .orElseGet(() -> columnFactory.createColumn(preferredColumn));
            if (tableColumn == null) {
                continue;
            }

            int targetIndex = i + 1;
            int currentIndex = table.getColumns().indexOf(tableColumn);
            if (currentIndex == -1) {
                table.getColumns().add(targetIndex, tableColumn);
            } else if (currentIndex != targetIndex) {
                table.getColumns().remove(currentIndex);
                table.getColumns().add(targetIndex, tableColumn);
            }
        }

        updateColumnResizePolicy(mainTablePreferences.getResizeColumnsToFit());
    }

    private Optional<TableColumn<BibEntryTableViewModel, ?>> findColumn(MainTableColumnModel preferredColumn) {
        for (TableColumn<BibEntryTableViewModel, ?> column : table.getColumns()) {
            if ((column instanceof MainTableColumn<?> mainTableColumn)
                    && preferredColumn.equals(mainTableColumn.getModel())) {
                return Optional.of(column);
            }
        }

        return Optional.empty();
    }

    private void updateColumnResizePolicy(boolean resizeColumnsToFit) {
        if (resizeColumnsToFit) {
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
        } else {
            table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        }
    }

    private boolean matchesConfiguredColumns() {
        List<MainTableColumnModel> currentColumns = getConfiguredColumns(table.getColumns());
        List<MainTableColumnModel> preferredColumns = getConfiguredColumns();

        if (currentColumns.size() != preferredColumns.size()) {
            return false;
        }

        for (int i = 0; i < currentColumns.size(); i++) {
            if (!matchesConfiguration(currentColumns.get(i), preferredColumns.get(i))) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesConfiguredSortOrder() {
        List<MainTableColumnModel> currentSortOrder = getConfiguredColumns(table.getSortOrder());
        List<MainTableColumnModel> preferredSortOrder = getConfiguredSortOrder();

        if (currentSortOrder.size() != preferredSortOrder.size()) {
            return false;
        }

        for (int i = 0; i < currentSortOrder.size(); i++) {
            if (!matchesConfiguration(currentSortOrder.get(i), preferredSortOrder.get(i))) {
                return false;
            }
        }

        return true;
    }

    private List<MainTableColumnModel> getConfiguredColumns() {
        return mainTablePreferences.getColumnPreferences().getColumns().stream()
                                   .filter(MainTableColumnModel::isConfigurable)
                                   .toList();
    }

    private List<MainTableColumnModel> getConfiguredSortOrder() {
        return mainTablePreferences.getColumnPreferences().getColumnSortOrder().stream()
                                   .filter(MainTableColumnModel::isConfigurable)
                                   .toList();
    }

    private List<MainTableColumnModel> getConfiguredColumns(List<TableColumn<BibEntryTableViewModel, ?>> columns) {
        return columns.stream()
                      .filter(MainTableColumn.class::isInstance)
                      .map(column -> ((MainTableColumn<?>) column).getModel())
                      .filter(MainTableColumnModel::isConfigurable)
                      .toList();
    }

    private boolean matchesConfiguration(MainTableColumnModel currentColumn, MainTableColumnModel preferredColumn) {
        return currentColumn.equals(preferredColumn)
                && Double.compare(currentColumn.getWidth(), preferredColumn.getWidth()) == 0
                && currentColumn.getSortType() == preferredColumn.getSortType();
    }
}
