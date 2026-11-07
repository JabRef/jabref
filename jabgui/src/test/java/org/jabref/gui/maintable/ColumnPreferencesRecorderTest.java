package org.jabref.gui.maintable;

import java.util.List;

import javafx.beans.InvalidationListener;
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

@NullMarked
@ExtendWith(JavaFxExtension.class)
class ColumnPreferencesRecorderTest {

    private TableView<BibEntryTableViewModel> table;
    private ColumnPreferences preferences;
    private MainTableColumnModel titleColumn;
    private MainTableColumnModel relevanceColumn;
    private ColumnPreferencesRecorder columnPreferencesRecorder;
    private int columnPreferenceChanges;

    @BeforeEach
    void setUp() {
        titleColumn = new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, StandardField.TITLE.getName());
        relevanceColumn = new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, SpecialField.RANKING.getName());

        table = new TableView<>();
        table.getColumns().setAll(List.of(
                new MainTableColumn<>(new MainTableColumnModel(MainTableColumnModel.Type.MATCH_CATEGORY)),
                new MainTableColumn<>(titleColumn),
                new MainTableColumn<>(relevanceColumn)));

        preferences = new ColumnPreferences(List.of(titleColumn, relevanceColumn), List.of(titleColumn));
        columnPreferencesRecorder = new ColumnPreferencesRecorder(table, preferences);
        columnPreferencesRecorder.bind();
        preferences.getColumns().addListener((InvalidationListener) _ -> columnPreferenceChanges++);
    }

    @Test
    void removingColumnUpdatesPreferences() {
        table.getColumns().remove(2);

        assertEquals(List.of(titleColumn), preferences.getColumns());
    }

    /// Widths are persisted only if table columns and preferences share the model instances
    @Test
    void storesModelInstancesOfTable() {
        preferences.setColumns(List.of(MainTableColumnModel.parse(titleColumn.getName())));

        table.getColumns().remove(2);

        assertSame(titleColumn, preferences.getColumns().getFirst());
    }

    @Test
    void columnWidthChangeUpdatesPreferences() {
        titleColumn.widthProperty().set(250);

        assertEquals(1, columnPreferenceChanges);
    }

    @Test
    void removedColumnWidthChangeDoesNotUpdatePreferences() {
        table.getColumns().remove(2);
        columnPreferenceChanges = 0;

        relevanceColumn.widthProperty().set(250);

        assertEquals(0, columnPreferenceChanges);
    }

    @Test
    void unboundTableStopsUpdatingPreferences() {
        columnPreferencesRecorder.unbind();

        table.getColumns().remove(2);

        assertEquals(List.of(titleColumn, relevanceColumn), preferences.getColumns());
    }
}
