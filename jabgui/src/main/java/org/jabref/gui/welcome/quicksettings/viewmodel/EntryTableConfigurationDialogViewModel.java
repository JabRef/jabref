package org.jabref.gui.welcome.quicksettings.viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.entry.field.InternalField;

public class EntryTableConfigurationDialogViewModel extends AbstractViewModel {
    private final BooleanProperty showCitationKeyProperty = new SimpleBooleanProperty();

    private final ColumnPreferences columnPreferences;

    public EntryTableConfigurationDialogViewModel(GuiPreferences preferences) {
        this.columnPreferences = preferences.getMainTablePreferences().getColumnPreferences();

        initializeSettings();
    }

    private void initializeSettings() {
        boolean isCitationKeyVisible = columnPreferences
                .getColumns()
                .stream()
                .anyMatch(column -> column.getType() == MainTableColumnModel.Type.NORMALFIELD
                        && InternalField.KEY_FIELD.getName().equals(column.getQualifier()));

        showCitationKeyProperty.set(isCitationKeyVisible);
    }

    public BooleanProperty showCitationKeyProperty() {
        return showCitationKeyProperty;
    }

    public boolean isShowCitationKey() {
        return showCitationKeyProperty.get();
    }

    public void saveSettings() {
        boolean isCitationKeyVisible = columnPreferences
                .getColumns()
                .stream()
                .anyMatch(column -> column.getType() == MainTableColumnModel.Type.NORMALFIELD
                        && InternalField.KEY_FIELD.getName().equals(column.getQualifier()));

        if (isShowCitationKey() && !isCitationKeyVisible) {
            MainTableColumnModel citationKeyColumn = new MainTableColumnModel(
                    MainTableColumnModel.Type.NORMALFIELD,
                    InternalField.KEY_FIELD.getName()
            );
            columnPreferences.getColumns().addFirst(citationKeyColumn);
        } else if (!isShowCitationKey() && isCitationKeyVisible) {
            columnPreferences.getColumns().removeIf(column ->
                    column.getType() == MainTableColumnModel.Type.NORMALFIELD
                            && InternalField.KEY_FIELD.getName().equals(column.getQualifier()));
        }
    }
}
