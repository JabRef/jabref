package org.jabref.gui.maintable;

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

@NullMarked
@ExtendWith(JavaFxExtension.class)
class PersistenceVisualStateTableTest {

    private TableView<BibEntryTableViewModel> table;
    private CountingColumnPreferences preferences;
    private PersistenceVisualStateTable persistenceVisualStateTable;
    private MainTableColumnModel titleColumn;
    private MainTableColumnModel relevanceColumn;

    @BeforeEach
    void setUp() {
        titleColumn = new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, StandardField.TITLE.getName());
        relevanceColumn = new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, SpecialField.RANKING.getName());

        table = new TableView<>();
        table.getColumns().setAll(List.of(
                new MainTableColumn<>(new MainTableColumnModel(MainTableColumnModel.Type.MATCH_CATEGORY)),
                new MainTableColumn<>(titleColumn),
                new MainTableColumn<>(relevanceColumn)));

        preferences = new CountingColumnPreferences(List.of(titleColumn, relevanceColumn), List.of(titleColumn));
        persistenceVisualStateTable = new PersistenceVisualStateTable(table, preferences);
        persistenceVisualStateTable.addListeners();
        preferences.resetCounters();
    }

    @Test
    void removedColumnModelsStopUpdatingPreferences() {
        table.getColumns().remove(2);
        preferences.resetCounters();

        relevanceColumn.widthProperty().set(250);

        assertEquals(0, preferences.columnUpdates());
    }

    @Test
    void disposeRemovesColumnModelListeners() {
        persistenceVisualStateTable.dispose();

        titleColumn.widthProperty().set(250);
        titleColumn.sortTypeProperty().set(TableColumn.SortType.DESCENDING);

        assertEquals(0, preferences.columnUpdates());
    }

    private static final class CountingColumnPreferences extends ColumnPreferences {

        private int columnUpdates;
        private int sortOrderUpdates;

        CountingColumnPreferences(List<MainTableColumnModel> columns,
                                  List<MainTableColumnModel> columnSortOrder) {
            super(columns, columnSortOrder);
        }

        @Override
        public void setColumns(List<MainTableColumnModel> list) {
            columnUpdates++;
            super.setColumns(list);
        }

        @Override
        public void setColumnSortOrder(List<MainTableColumnModel> list) {
            sortOrderUpdates++;
            super.setColumnSortOrder(list);
        }

        int columnUpdates() {
            return columnUpdates;
        }

        @SuppressWarnings("unused")
        int sortOrderUpdates() {
            return sortOrderUpdates;
        }

        void resetCounters() {
            columnUpdates = 0;
            sortOrderUpdates = 0;
        }
    }
}
