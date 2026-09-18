package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
@ExtendWith(JavaFxExtension.class)
class ColumnPreferencesApplierTest {

    private TableView<BibEntryTableViewModel> table;
    private ColumnPreferences columnPreferences;
    private MainTablePreferences mainTablePreferences;
    private ColumnPreferencesApplier applier;
    private MainTableColumnFactory columnFactory;
    private MainTableColumnModel titleColumn;
    private MainTableColumnModel yearColumn;
    private MainTableColumnModel relevanceColumn;

    @BeforeEach
    void setUp() {
        titleColumn = new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, StandardField.TITLE.getName());
        yearColumn = new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, StandardField.YEAR.getName());
        relevanceColumn = new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, SpecialField.RANKING.getName());

        columnPreferences = new ColumnPreferences(List.of(titleColumn, yearColumn), List.of(titleColumn));
        mainTablePreferences = new MainTablePreferences(columnPreferences, false, false);
        table = new TableView<>();

        columnFactory = mock(MainTableColumnFactory.class);
        when(columnFactory.createColumn(any(MainTableColumnModel.class))).thenAnswer(invocation -> new MainTableColumn<>((MainTableColumnModel) invocation.getArgument(0)));
        when(columnFactory.createColumns()).thenAnswer(_ -> createColumns(columnPreferences.getColumns()));

        // Same order as in MainTable, so that the write-back of the table is part of every test
        applier = new ColumnPreferencesApplier(table, columnFactory, mainTablePreferences);
        applier.bind();
        applier.applySortOrder();
        new ColumnPreferencesRecorder(table, columnPreferences).bind();
    }

    @Test
    void bindShowsConfiguredColumnsAfterMatchCategoryColumn() {
        assertEquals(MainTableColumnModel.Type.MATCH_CATEGORY, modelOf(table.getColumns().getFirst()).getType());
        assertEquals(List.of(titleColumn, yearColumn), visibleColumns());
    }

    @Test
    void applySortOrderPutsMatchCategoryColumnFirst() {
        assertEquals(List.of(table.getColumns().getFirst(), table.getColumns().get(1)), table.getSortOrder());
    }

    @Test
    void updatesDisplayedColumnsWhenColumnPreferencesChange() {
        columnPreferences.setColumns(List.of(relevanceColumn, titleColumn));

        assertEquals(List.of(relevanceColumn, titleColumn), visibleColumns());
    }

    @Test
    void updatesSortOrderWhenSortOrderPreferencesChange() {
        columnPreferences.setColumnSortOrder(List.of(yearColumn, titleColumn));

        assertEquals(List.of(yearColumn, titleColumn), visibleSortOrder());
    }

    @Test
    void reorderingColumnsInOneOpenTableUpdatesOtherOpenTables() {
        BoundTable secondTable = newBoundTable();

        TableColumn<BibEntryTableViewModel, ?> yearColumnInFirstTable = findColumn(table, yearColumn);
        table.getColumns().remove(yearColumnInFirstTable);
        table.getColumns().add(1, yearColumnInFirstTable);

        assertEquals(List.of(yearColumn, titleColumn), visibleColumns(secondTable.table()));
    }

    @Test
    void sortingInOneOpenTableUpdatesOtherOpenTables() {
        BoundTable secondTable = newBoundTable();

        TableColumn<BibEntryTableViewModel, ?> yearColumnInFirstTable = findColumn(table, yearColumn);
        yearColumnInFirstTable.setSortType(TableColumn.SortType.DESCENDING);
        table.getSortOrder().setAll(List.of(table.getColumns().getFirst(), yearColumnInFirstTable));

        assertEquals(List.of(yearColumn), visibleSortOrder(secondTable.table()));
        assertEquals(TableColumn.SortType.DESCENDING, findColumn(secondTable.table(), yearColumn).getSortType());
    }

    @Test
    void keepsExistingColumnsWhenAddingAnotherConfiguredColumn() {
        TableColumn<BibEntryTableViewModel, ?> originalTitleColumn = table.getColumns().get(1);

        columnPreferences.setColumns(List.of(titleColumn, yearColumn, relevanceColumn));

        assertSame(originalTitleColumn, table.getColumns().get(1));
    }

    @Test
    void keepsMatchCategoryColumnInSortOrderWhenSortedColumnIsRemoved() {
        columnPreferences.setColumns(List.of(yearColumn));

        assertEquals(List.of(table.getColumns().getFirst()), table.getSortOrder());
    }

    @Test
    void ignoresReservedColumnsInPreferences() {
        MainTableColumnModel reservedColumn = new MainTableColumnModel(MainTableColumnModel.Type.MATCH_CATEGORY);

        columnPreferences.setColumns(List.of(titleColumn, reservedColumn, relevanceColumn));

        assertEquals(List.of(titleColumn, relevanceColumn), visibleColumns());
    }

    @Test
    void preferencesSurviveRoundTripThroughTable() {
        columnPreferences.setColumns(List.of(relevanceColumn, titleColumn));
        columnPreferences.setColumnSortOrder(List.of(relevanceColumn));

        assertEquals(List.of(relevanceColumn, titleColumn), columnPreferences.getColumns());
        assertEquals(List.of(relevanceColumn), columnPreferences.getColumnSortOrder());
    }

    @Test
    void tableSurvivesRoundTripThroughPreferences() {
        TableColumn<BibEntryTableViewModel, ?> yearTableColumn = table.getColumns().get(2);
        List<TableColumn<BibEntryTableViewModel, ?>> expectedColumns = List.of(table.getColumns().getFirst(), yearTableColumn, table.getColumns().get(1));

        // Reordering by drag and drop, as JavaFX does it
        table.getColumns().remove(yearTableColumn);
        table.getColumns().add(1, yearTableColumn);

        assertEquals(expectedColumns, table.getColumns());
        assertEquals(List.of(yearColumn, titleColumn), columnPreferences.getColumns());
    }

    /// Widths are persisted only if table columns and preferences share the model instances
    @Test
    void columnsAddedByPreferencesUseModelInstancesOfPreferences() {
        columnPreferences.setColumns(List.of(titleColumn, yearColumn, relevanceColumn));

        assertSame(relevanceColumn, modelOf(table.getColumns().get(3)));
        assertSame(relevanceColumn, columnPreferences.getColumns().get(2));
    }

    @Test
    void unboundApplierStopsReactingToPreferenceChanges() {
        applier.unbind();

        columnPreferences.setColumns(List.of(relevanceColumn));

        assertEquals(List.of(titleColumn, yearColumn), visibleColumns());
    }

    @Test
    void updatesResizePolicyWhenPreferenceChanges() {
        mainTablePreferences.setResizeColumnsToFit(true);

        assertEquals(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS, table.getColumnResizePolicy());
    }

    /// Mirrors [MainTableColumnFactory#createColumns()]
    private static List<TableColumn<BibEntryTableViewModel, ?>> createColumns(List<MainTableColumnModel> configuredColumns) {
        List<TableColumn<BibEntryTableViewModel, ?>> columns = new ArrayList<>();
        columns.add(new MainTableColumn<>(new MainTableColumnModel(MainTableColumnModel.Type.MATCH_CATEGORY)));
        configuredColumns.stream()
                         .filter(MainTableColumnModel::isConfigurable)
                         .forEach(model -> columns.add(new MainTableColumn<>(model)));
        return columns;
    }

    private BoundTable newBoundTable() {
        TableView<BibEntryTableViewModel> additionalTable = new TableView<>();
        ColumnPreferencesApplier additionalApplier = new ColumnPreferencesApplier(additionalTable, columnFactory, mainTablePreferences);
        additionalApplier.bind();
        additionalApplier.applySortOrder();

        ColumnPreferencesRecorder additionalRecorder = new ColumnPreferencesRecorder(additionalTable, columnPreferences);
        additionalRecorder.bind();
        return new BoundTable(additionalTable, additionalApplier, additionalRecorder);
    }

    private static MainTableColumnModel modelOf(TableColumn<BibEntryTableViewModel, ?> column) {
        return ((MainTableColumn<?>) column).getModel();
    }

    private static TableColumn<BibEntryTableViewModel, ?> findColumn(TableView<BibEntryTableViewModel> table,
                                                                     MainTableColumnModel model) {
        return table.getColumns().stream()
                    .filter(column -> column instanceof MainTableColumn<?> mainTableColumn
                            && model.equals(mainTableColumn.getModel()))
                    .findFirst()
                    .orElseThrow();
    }

    private List<MainTableColumnModel> visibleColumns() {
        return visibleColumns(table);
    }

    private static List<MainTableColumnModel> visibleColumns(TableView<BibEntryTableViewModel> table) {
        return ColumnPreferencesRecorder.toPersistedModels(table.getColumns());
    }

    private List<MainTableColumnModel> visibleSortOrder() {
        return visibleSortOrder(table);
    }

    private static List<MainTableColumnModel> visibleSortOrder(TableView<BibEntryTableViewModel> table) {
        return ColumnPreferencesRecorder.toPersistedModels(table.getSortOrder());
    }

    private record BoundTable(TableView<BibEntryTableViewModel> table,
                              ColumnPreferencesApplier applier,
                              ColumnPreferencesRecorder recorder) {
    }
}
