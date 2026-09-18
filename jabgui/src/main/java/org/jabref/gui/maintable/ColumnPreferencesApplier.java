package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.maintable.columns.MainTableColumn;

import org.jspecify.annotations.NullMarked;

/// Applies the column preferences (columns, sort order, resize policy) to the main table: initially and whenever
/// they change, e.g., in the preferences dialog.
///
/// This is the preferences-to-table direction. The opposite direction is [ColumnPreferencesRecorder].
/// Both directions compare columns by [ColumnPreferencesRecorder#toPersistedModels(List)]. Each change is
/// applied with a single `setAll`, so that the write-back of the table equals the preferences and ends the round trip.
///
/// All main tables share the same preferences. Hence, reordering, adding, removing or sorting columns in one library
/// tab is applied to the main tables of all other open library tabs as well. This is intended: the column preferences
/// are global, so all tables show the state that will be restored at the next start.
///
/// New columns are created from the model instances held by the preferences. Resized widths are persisted only
/// because table and preferences share these instances (see [ColumnPreferences]).
@NullMarked
class ColumnPreferencesApplier {

    private final TableView<BibEntryTableViewModel> table;
    private final MainTableColumnFactory columnFactory;
    private final MainTablePreferences mainTablePreferences;

    private final ListChangeListener<MainTableColumnModel> columnsListener = _ -> applyColumns();
    private final ListChangeListener<MainTableColumnModel> sortOrderListener = _ -> applySortOrder();
    private final ChangeListener<Boolean> resizeColumnsListener = (_, _, resizeColumnsToFit) -> applyResizePolicy(resizeColumnsToFit);

    ColumnPreferencesApplier(TableView<BibEntryTableViewModel> table,
                             MainTableColumnFactory columnFactory,
                             MainTablePreferences mainTablePreferences) {
        this.table = table;
        this.columnFactory = columnFactory;
        this.mainTablePreferences = mainTablePreferences;
    }

    /// Creates the columns, applies the resize policy and follows later changes of the preferences.
    ///
    /// The initial sort order is not applied here, because sorting a large library is expensive. The caller decides
    /// when to call [#applySortOrder()].
    void bind() {
        table.getColumns().setAll(columnFactory.createColumns());
        applyResizePolicy(mainTablePreferences.getResizeColumnsToFit());

        mainTablePreferences.getColumnPreferences().getColumns().addListener(columnsListener);
        mainTablePreferences.getColumnPreferences().getColumnSortOrder().addListener(sortOrderListener);
        mainTablePreferences.resizeColumnsToFitProperty().addListener(resizeColumnsListener);
    }

    /// The preferences outlive the table, so the listeners have to be removed when the library tab closes.
    void unbind() {
        mainTablePreferences.getColumnPreferences().getColumns().removeListener(columnsListener);
        mainTablePreferences.getColumnPreferences().getColumnSortOrder().removeListener(sortOrderListener);
        mainTablePreferences.resizeColumnsToFitProperty().removeListener(resizeColumnsListener);
    }

    void applySortOrder() {
        List<TableColumn<BibEntryTableViewModel, ?>> sortOrder = configured(mainTablePreferences.getColumnPreferences().getColumnSortOrder())
                .stream()
                .flatMap(model -> findColumn(model).stream())
                .collect(Collectors.toCollection(ArrayList::new));
        findMatchCategoryColumn().ifPresent(sortOrder::addFirst);
        if (sortOrder.equals(table.getSortOrder())) {
            return;
        }
        table.getSortOrder().setAll(sortOrder);
    }

    private void applyColumns() {
        List<MainTableColumnModel> configuredColumns = configured(mainTablePreferences.getColumnPreferences().getColumns());
        if (configuredColumns.equals(ColumnPreferencesRecorder.toPersistedModels(table.getColumns()))) {
            return;
        }

        List<TableColumn<BibEntryTableViewModel, ?>> columns = new ArrayList<>();
        // Reusing the instance keeps it in the sort order. A new instance would briefly empty the sort order.
        findMatchCategoryColumn().ifPresent(columns::add);
        configuredColumns.stream()
                         .flatMap(model -> findColumn(model)
                                 .or(() -> Optional.ofNullable(columnFactory.createColumn(model)))
                                 .stream())
                         .forEach(columns::add);
        table.getColumns().setAll(columns);
    }

    private void applyResizePolicy(boolean resizeColumnsToFit) {
        if (resizeColumnsToFit) {
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
        } else {
            table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        }
    }

    private static List<MainTableColumnModel> configured(List<MainTableColumnModel> models) {
        return models.stream()
                     .filter(MainTableColumnModel::isConfigurable)
                     .toList();
    }

    private Optional<TableColumn<BibEntryTableViewModel, ?>> findMatchCategoryColumn() {
        return table.getColumns().stream()
                    .filter(column -> column instanceof MainTableColumn<?> mainTableColumn
                            && mainTableColumn.getModel().getType() == MainTableColumnModel.Type.MATCH_CATEGORY)
                    .findFirst();
    }

    private Optional<TableColumn<BibEntryTableViewModel, ?>> findColumn(MainTableColumnModel model) {
        return table.getColumns().stream()
                    .filter(column -> column instanceof MainTableColumn<?> mainTableColumn
                            && model.equals(mainTableColumn.getModel()))
                    .findFirst();
    }
}
