package org.jabref.gui.preferences.table;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.maintable.MainTablePreferences;
import org.jabref.gui.maintable.NameDisplayPreferences;
import org.jabref.gui.specialfields.SpecialFieldsPreferences;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@NullMarked
@ExtendWith(JavaFxExtension.class)
class TableTabViewModelTest {

    @Test
    void insertColumnInListIgnoresReservedColumns() {
        MainTablePreferences mainTablePreferences = new MainTablePreferences(new ColumnPreferences(List.of(), List.of()), false, false);
        TableTabViewModel viewModel = newTableTabViewModel(mainTablePreferences);
        viewModel.addColumnProperty().set(MainTableColumnModel.parse("match_category"));

        viewModel.insertColumnInList();

        assertEquals(List.of(), viewModel.columnsListProperty().get());
    }

    @Test
    void storeSettingsFiltersReservedColumns() {
        MainTablePreferences mainTablePreferences = new MainTablePreferences(new ColumnPreferences(List.of(), List.of()), false, false);
        TableTabViewModel viewModel = newTableTabViewModel(mainTablePreferences);
        MainTableColumnModel titleColumn = new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, StandardField.TITLE.getName());
        MainTableColumnModel reservedColumn = new MainTableColumnModel(MainTableColumnModel.Type.MATCH_CATEGORY);

        viewModel.columnsListProperty().addAll(titleColumn, reservedColumn);
        viewModel.storeSettings();

        assertEquals(List.of(titleColumn), mainTablePreferences.getColumnPreferences().getColumns());
    }

    private TableTabViewModel newTableTabViewModel(MainTablePreferences mainTablePreferences) {
        return new TableTabViewModel(
                mock(DialogService.class),
                mock(SpecialFieldsPreferences.class),
                mock(NameDisplayPreferences.class),
                mainTablePreferences,
                mock(ExternalApplicationsPreferences.class));
    }
}
