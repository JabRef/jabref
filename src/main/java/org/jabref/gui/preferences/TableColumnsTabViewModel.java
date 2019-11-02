package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.SelectionModel;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.util.NoSelectionModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.JabRefPreferences;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;

public class TableColumnsTabViewModel implements PreferenceTabViewModel {

    private final ListProperty<TableColumnsItemModel> columnsListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<SelectionModel<TableColumnsItemModel>> selectedColumnModelProperty = new SimpleObjectProperty<>(new NoSelectionModel<>());
    private final ListProperty<MainTableColumnModel> availableColumnsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<MainTableColumnModel> addColumnProperty = new SimpleObjectProperty<>();
    private final BooleanProperty specialFieldsEnabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty specialFieldsSyncKeywordsProperty = new SimpleBooleanProperty();
    private final BooleanProperty specialFieldsSerializeProperty = new SimpleBooleanProperty();
    private final BooleanProperty extraFileColumnsEnabledProperty = new SimpleBooleanProperty();

    private FunctionBasedValidator columnsNotEmptyValidator;

    private List<String> restartWarnings = new ArrayList<>();

    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final ColumnPreferences columnPreferences;

    public TableColumnsTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.columnPreferences = preferences.getColumnPreferences();

        specialFieldsEnabledProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                insertSpecialFieldColumns();
            } else {
                removeSpecialFieldColumns();
            }
        });

        extraFileColumnsEnabledProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                insertExtraFileColumns();
            } else {
                removeExtraFileColumns();
            }
        });

        columnsNotEmptyValidator = new FunctionBasedValidator<>(
                columnsListProperty,
                list -> list.size() > 0,
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("Entry table columns"),
                        Localization.lang("Columns"),
                        Localization.lang("List must not be empty."))));
    }

    @Override
    public void setValues() {
        specialFieldsEnabledProperty.setValue(columnPreferences.getSpecialFieldsEnabled());
        specialFieldsSyncKeywordsProperty.setValue(columnPreferences.getAutoSyncSpecialFieldsToKeyWords());
        specialFieldsSerializeProperty.setValue(columnPreferences.getSerializeSpecialFields());
        extraFileColumnsEnabledProperty.setValue(columnPreferences.getExtraFileColumnsEnabled());

        fillColumnList();

        availableColumnsProperty.clear();

        availableColumnsProperty.add(new MainTableColumnModel(MainTableColumnModel.Type.LINKED_IDENTIFIER));
        availableColumnsProperty.add(new MainTableColumnModel(MainTableColumnModel.Type.GROUPS));
        availableColumnsProperty.add(new MainTableColumnModel(MainTableColumnModel.Type.FILES));
        availableColumnsProperty.add(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, InternalField.TIMESTAMP.getName()));
        availableColumnsProperty.add(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, InternalField.OWNER.getName()));
        availableColumnsProperty.add(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, InternalField.GROUPS.getName()));
        availableColumnsProperty.add(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, InternalField.KEY_FIELD.getName()));
        availableColumnsProperty.add(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, InternalField.TYPE_HEADER.getName()));
        availableColumnsProperty.add(new MainTableColumnModel(MainTableColumnModel.Type.LINKED_IDENTIFIER));

        EnumSet.allOf(StandardField.class).stream()
               .map(Field::getName)
               .map(name -> new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, name))
               .forEach(item -> availableColumnsProperty.getValue().add(item));

        if (specialFieldsEnabledProperty.getValue()) {
            insertSpecialFieldColumns();
        }

        if (extraFileColumnsEnabledProperty.getValue()) {
            insertExtraFileColumns();
        }
    }

    public void fillColumnList() {
        columnsListProperty.getValue().clear();

        columnPreferences.getColumns().stream()
                         .map(column -> new TableColumnsItemModel(
                                 column,
                                 columnPreferences.getColumnWidth(column.toString())))
                         .forEach(columnsListProperty.getValue()::add);
    }

    private void insertSpecialFieldColumns() {
        EnumSet.allOf(SpecialField.class).stream()
               .map(Field::getName).map(name -> new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, name))
               .forEach(item -> availableColumnsProperty.getValue().add(0, item));
    }

    private void removeSpecialFieldColumns() {
        columnsListProperty.getValue().removeIf(column -> column.getColumnModel().getType().equals(MainTableColumnModel.Type.SPECIALFIELD));
        availableColumnsProperty.getValue().removeIf(columnName -> columnName.getType().equals(MainTableColumnModel.Type.SPECIALFIELD));
    }

    private void insertExtraFileColumns() {
        ExternalFileTypes.getInstance().getExternalFileTypeSelection().stream()
                         .map(ExternalFileType::getName)
                         .map(name -> new MainTableColumnModel(MainTableColumnModel.Type.EXTRAFILE, name))
                         .forEach(item -> availableColumnsProperty.getValue().add(item));
    }

    private void removeExtraFileColumns() {
        columnsListProperty.getValue().removeIf(column -> column.getColumnModel().getType() == MainTableColumnModel.Type.EXTRAFILE);
        availableColumnsProperty.getValue().removeIf(columnName -> columnName.getType().equals(MainTableColumnModel.Type.EXTRAFILE));
    }

    public void insertColumnInList() {
        if (addColumnProperty.getValue() == null) {
            return;
        }

        if (columnsListProperty.getValue().stream().map(TableColumnsItemModel::getColumnModel).filter(item -> item.equals(addColumnProperty.getValue())).findAny().isEmpty()) {
            columnsListProperty.add(new TableColumnsItemModel(addColumnProperty.getValue()));
            addColumnProperty.setValue(null);
        }
    }

    public void removeColumn(TableColumnsItemModel column) {
        columnsListProperty.remove(column);
    }

    public void moveColumnUp() {
        TableColumnsItemModel selectedColumn = selectedColumnModelProperty.getValue().getSelectedItem();
        int row = columnsListProperty.getValue().indexOf(selectedColumn);
        if (selectedColumn == null || row < 1) {
            return;
        }

        columnsListProperty.remove(selectedColumn);
        columnsListProperty.add(row - 1, selectedColumn);
        selectedColumnModelProperty.getValue().clearAndSelect(row - 1);
    }

    public void moveColumnDown() {
        TableColumnsItemModel selectedColumn = selectedColumnModelProperty.getValue().getSelectedItem();
        int row = columnsListProperty.getValue().indexOf(selectedColumn);
        if (selectedColumn == null || row > columnsListProperty.getValue().size() - 2) {
            return;
        }

        columnsListProperty.remove(selectedColumn);
        columnsListProperty.add(row + 1, selectedColumn);
        selectedColumnModelProperty.getValue().clearAndSelect(row + 1);
    }

    @Override
    public void storeSettings() {
        List<MainTableColumnModel> columns = columnsListProperty.stream()
                                                                .map(TableColumnsItemModel::getColumnModel)
                                                                .collect(Collectors.toList());

        // for each column get either actual width or - if it does not exist - default value
        Map<String, Double> columnWidths = new HashMap<>();
        columns.forEach(column -> columnWidths.put(column.toString(),columnPreferences.getColumnWidth(column.toString())));

        ColumnPreferences newColumnPreferences = new ColumnPreferences(
                columns,
                specialFieldsEnabledProperty.getValue(),
                specialFieldsSyncKeywordsProperty.getValue(),
                specialFieldsSerializeProperty.getValue(),
                extraFileColumnsEnabledProperty.getValue(),
                columnWidths,
                columnPreferences.getSortTypesForColumns()
        );

        if (columnPreferences.getAutoSyncSpecialFieldsToKeyWords() != newColumnPreferences.getAutoSyncSpecialFieldsToKeyWords()) {
            restartWarnings.add(Localization.lang("Synchronize special fields to keywords"));
        }

        if (columnPreferences.getSerializeSpecialFields() != newColumnPreferences.getSerializeSpecialFields()) {
            restartWarnings.add(Localization.lang("Serialize special fields"));
        }

        preferences.storeColumnPreferences(newColumnPreferences);
    }

    ValidationStatus columnsListValidationStatus() {
        return columnsNotEmptyValidator.getValidationStatus();
    }

    @Override
    public boolean validateSettings() {
        ValidationStatus status = columnsListValidationStatus();
        if (!status.isValid() && status.getHighestMessage().isPresent()) {
            dialogService.showErrorDialogAndWait(status.getHighestMessage().get().getMessage());
            return false;
        }
        return true;
    }

    @Override
    public List<String> getRestartWarnings() {
        return restartWarnings;
    }

    public ListProperty<TableColumnsItemModel> columnsListProperty() { return this.columnsListProperty; }

    public ObjectProperty<SelectionModel<TableColumnsItemModel>> selectedColumnModelProperty() { return selectedColumnModelProperty; }

    public ListProperty<MainTableColumnModel> availableColumnsProperty() { return this.availableColumnsProperty; }

    public ObjectProperty<MainTableColumnModel> addColumnProperty() { return this.addColumnProperty; }

    public BooleanProperty specialFieldsEnabledProperty() { return this.specialFieldsEnabledProperty; }

    public BooleanProperty specialFieldsSyncKeywordsProperty() { return this.specialFieldsSyncKeywordsProperty; }

    public BooleanProperty specialFieldsSerializeProperty() { return this.specialFieldsSerializeProperty; }

    public BooleanProperty extraFileColumnsEnabledProperty() { return this.extraFileColumnsEnabledProperty; }

}
