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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
@ExtendWith(JavaFxExtension.class)
class MainTableColumnPreferenceSynchronizerTest {

    private TableView<BibEntryTableViewModel> table;
    private MainTablePreferences mainTablePreferences;
    private MainTableColumnPreferenceSynchronizer synchronizer;
    private MainTableColumnModel titleColumn;
    private MainTableColumnModel relevanceColumn;

    @BeforeEach
    void setUp() {
        titleColumn = new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, StandardField.TITLE.getName());
        relevanceColumn = new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, SpecialField.RANKING.getName());

        ColumnPreferences columnPreferences = new ColumnPreferences(List.of(titleColumn), List.of(titleColumn));
        mainTablePreferences = new MainTablePreferences(columnPreferences, false, false);
        table = new TableView<>();

        MainTableColumnFactory columnFactory = mock(MainTableColumnFactory.class);
        when(columnFactory.createColumns()).thenAnswer(_ -> createColumns(mainTablePreferences.getColumnPreferences().getColumns()));
        when(columnFactory.createColumn(any(MainTableColumnModel.class))).thenAnswer(invocation -> new MainTableColumn<>((MainTableColumnModel) invocation.getArgument(0)));

        PersistenceVisualStateTable persistenceVisualStateTable = new PersistenceVisualStateTable(table, mainTablePreferences.getColumnPreferences());
        synchronizer = new MainTableColumnPreferenceSynchronizer(table, columnFactory, mainTablePreferences, persistenceVisualStateTable);
        synchronizer.initializeColumns();
        synchronizer.restoreConfiguredSortOrder();
        synchronizer.addListeners();
    }

    @Test
    void updatesDisplayedColumnsWhenColumnPreferencesChange() {
        mainTablePreferences.getColumnPreferences().setColumns(List.of(titleColumn, relevanceColumn));

        assertEquals(List.of(titleColumn, relevanceColumn), visibleColumns());
    }

    @Test
    void updatesConfiguredSortOrderWhenPreferencesChange() {
        mainTablePreferences.getColumnPreferences().setColumns(List.of(titleColumn, relevanceColumn));
        mainTablePreferences.getColumnPreferences().setColumnSortOrder(List.of(relevanceColumn));

        assertEquals(List.of(relevanceColumn), visibleSortOrder());
    }

    @Test
    void keepsExistingColumnsWhenAddingAnotherConfiguredColumn() {
        TableColumn<BibEntryTableViewModel, ?> originalTitleColumn = table.getColumns().get(1);

        mainTablePreferences.getColumnPreferences().setColumns(List.of(titleColumn, relevanceColumn));

        assertEquals(originalTitleColumn, table.getColumns().get(1));
    }

    @Test
    void ignoresReservedColumnsInPreferences() {
        MainTableColumnModel reservedColumn = new MainTableColumnModel(MainTableColumnModel.Type.MATCH_CATEGORY);

        mainTablePreferences.getColumnPreferences().setColumns(List.of(titleColumn, reservedColumn, relevanceColumn));
        mainTablePreferences.getColumnPreferences().setColumnSortOrder(List.of(reservedColumn, relevanceColumn));

        assertEquals(List.of(titleColumn, relevanceColumn), visibleColumns());
        assertEquals(List.of(relevanceColumn), visibleSortOrder());
    }

    @Test
    void disposedSynchronizerStopsReactingToPreferenceChanges() {
        synchronizer.dispose();

        mainTablePreferences.getColumnPreferences().setColumns(List.of(titleColumn, relevanceColumn));

        assertEquals(List.of(titleColumn), visibleColumns());
    }

    @Test
    void updatesResizePolicyWhenPreferenceChanges() {
        mainTablePreferences.setResizeColumnsToFit(true);

        assertEquals(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS, table.getColumnResizePolicy());
    }

    private List<TableColumn<BibEntryTableViewModel, ?>> createColumns(List<MainTableColumnModel> configuredColumns) {
        List<TableColumn<BibEntryTableViewModel, ?>> columns = new ArrayList<>();
        columns.add(new MainTableColumn<>(new MainTableColumnModel(MainTableColumnModel.Type.MATCH_CATEGORY)));
        configuredColumns.stream()
                         .filter(MainTableColumnModel::isConfigurable)
                         .forEach(column -> columns.add(new MainTableColumn<>(column)));
        return columns;
    }

    private List<MainTableColumnModel> visibleColumns() {
        return table.getColumns().stream()
                    .map(column -> ((MainTableColumn<?>) column).getModel())
                    .filter(model -> model.getType() != MainTableColumnModel.Type.MATCH_CATEGORY)
                    .toList();
    }

    private List<MainTableColumnModel> visibleSortOrder() {
        return table.getSortOrder().stream()
                    .map(column -> ((MainTableColumn<?>) column).getModel())
                    .filter(model -> model.getType() != MainTableColumnModel.Type.MATCH_CATEGORY)
                    .toList();
    }
}
