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
import org.jabref.gui.util.FieldsUtil;
import org.jabref.gui.util.NoSelectionModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
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
    private final ListProperty<Field> availableColumnsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<Field> addColumnProperty = new SimpleObjectProperty<>();
    private final BooleanProperty specialFieldsEnabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty specialFieldsSyncKeywordsProperty = new SimpleBooleanProperty();
    private final BooleanProperty specialFieldsSerializeProperty = new SimpleBooleanProperty();
    private final BooleanProperty showFileColumnProperty = new SimpleBooleanProperty();
    private final BooleanProperty showUrlColumnProperty = new SimpleBooleanProperty();
    private final BooleanProperty preferUrlProperty = new SimpleBooleanProperty();
    private final BooleanProperty preferDoiProperty = new SimpleBooleanProperty();
    private final BooleanProperty showEPrintColumnProperty = new SimpleBooleanProperty();
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
        showFileColumnProperty.setValue(columnPreferences.showFileColumn());
        showUrlColumnProperty.setValue(columnPreferences.showUrlColumn());
        preferUrlProperty.setValue(!columnPreferences.preferDoiOverUrl());
        preferDoiProperty.setValue(columnPreferences.preferDoiOverUrl());
        showEPrintColumnProperty.setValue(columnPreferences.showEprintColumn());
        specialFieldsEnabledProperty.setValue(columnPreferences.getSpecialFieldsEnabled());
        specialFieldsSyncKeywordsProperty.setValue(columnPreferences.getAutoSyncSpecialFieldsToKeyWords());
        specialFieldsSerializeProperty.setValue(columnPreferences.getSerializeSpecialFields());
        extraFileColumnsEnabledProperty.setValue(columnPreferences.getExtraFileColumnsEnabled());

        fillColumnList();

        availableColumnsProperty.clear();

        availableColumnsProperty.add(InternalField.TIMESTAMP);
        availableColumnsProperty.add(InternalField.OWNER);
        availableColumnsProperty.add(InternalField.GROUPS);
        availableColumnsProperty.add(InternalField.KEY_FIELD);
        availableColumnsProperty.add(InternalField.TYPE_HEADER);

        EnumSet.allOf(StandardField.class).forEach(item -> availableColumnsProperty.getValue().add(item));

        if (specialFieldsEnabledProperty.getValue()) {
            insertSpecialFieldColumns();
        }

        if (extraFileColumnsEnabledProperty.getValue()) {
            insertExtraFileColumns();
        }
    }

    public void fillColumnList() {
        columnsListProperty.getValue().clear();

        columnPreferences.getColumnNames().stream()
                .map(FieldFactory::parseField)
                .map(field -> new TableColumnsItemModel(field, columnPreferences.getColumnWidth(field.getName())))
                .forEach(columnsListProperty.getValue()::add);
    }

    private void insertSpecialFieldColumns() {
        EnumSet.allOf(SpecialField.class).forEach(item -> availableColumnsProperty.getValue().add(0, item));
    }

    private void removeSpecialFieldColumns() {
        columnsListProperty.getValue().removeIf(column -> column.getField() instanceof SpecialField);
        availableColumnsProperty.getValue().removeIf(field -> field instanceof SpecialField);
    }

    private void insertExtraFileColumns() {
        ExternalFileTypes.getInstance().getExternalFileTypeSelection().stream()
                .map(ExternalFileType::getName)
                .map(FieldsUtil.ExtraFilePseudoField::new)
                .forEach(availableColumnsProperty::add);
    }

    private void removeExtraFileColumns() {
        columnsListProperty.getValue().removeIf(column -> column.getField() instanceof FieldsUtil.ExtraFilePseudoField);
        availableColumnsProperty.getValue().removeIf(field -> field instanceof FieldsUtil.ExtraFilePseudoField);
    }

    public void insertColumnInList() {
        if (addColumnProperty.getValue() == null) {
            return;
        }

        if (columnsListProperty.getValue().stream().filter(item -> item.getField().equals(addColumnProperty.getValue())).findAny().isEmpty()) {
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
        List<String> columnNames = columnsListProperty.stream()
                .map(item -> item.getField().getName()).collect(Collectors.toList());

        // for each column get either actual width or - if it does not exist - default value
        Map<String,Double> columnWidths = new HashMap<>();
        columnNames.forEach(field -> columnWidths.put(field,columnPreferences.getColumnWidth(field)));

        ColumnPreferences newColumnPreferences = new ColumnPreferences(
                showFileColumnProperty.getValue(),
                showUrlColumnProperty.getValue(),
                preferDoiProperty.getValue(),
                showEPrintColumnProperty.getValue(),
                columnNames,
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

    public ListProperty<Field> availableColumnsProperty() { return this.availableColumnsProperty; }

    public ObjectProperty<Field> addColumnProperty() { return this.addColumnProperty; }

    public BooleanProperty showFileColumnProperty() { return this.showFileColumnProperty; }

    public BooleanProperty showUrlColumnProperty() { return this.showUrlColumnProperty; }

    public BooleanProperty preferUrlProperty() { return this.preferUrlProperty; }

    public BooleanProperty preferDoiProperty() { return this.preferDoiProperty; }

    public BooleanProperty specialFieldsEnabledProperty() { return this.specialFieldsEnabledProperty; }

    public BooleanProperty specialFieldsSyncKeywordsProperty() { return this.specialFieldsSyncKeywordsProperty; }

    public BooleanProperty specialFieldsSerializeProperty() { return this.specialFieldsSerializeProperty; }

    public BooleanProperty showEPrintColumnProperty() { return this.showEPrintColumnProperty; }

    public BooleanProperty extraFileColumnsEnabledProperty() { return this.extraFileColumnsEnabledProperty; }

}
