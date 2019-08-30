package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.util.NoSelectionModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.JabRefPreferences;

public class TableColumnsTabViewModel implements PreferenceTabViewModel {

    private final ListProperty<TableColumnsItemModel> columnsListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<SelectionModel<TableColumnsItemModel>> selectedColumnModelProperty = new SimpleObjectProperty<>(new NoSelectionModel<>());

    private final ListProperty<Field> selectableFieldProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final SimpleObjectProperty<Field> addFieldProperty = new SimpleObjectProperty<>();

    private final SimpleBooleanProperty specialFieldsEnabledProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldsSyncKeyWordsProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldsSerializeProperty = new SimpleBooleanProperty();

    private final SimpleBooleanProperty showFileColumnProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty showUrlColumnProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty preferUrlProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty preferDoiProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty showEPrintColumnProperty = new SimpleBooleanProperty();

    private final SimpleBooleanProperty showExtraFileColumnsProperty = new SimpleBooleanProperty();

    private List<String> restartWarnings = new ArrayList<>();

    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final ColumnPreferences columnPreferences;
    private final JabRefFrame frame;

    public TableColumnsTabViewModel(DialogService dialogService, JabRefPreferences preferences, JabRefFrame frame) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.columnPreferences = preferences.getColumnPreferences();
        this.frame = frame;

        specialFieldsEnabledProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                insertSpecialColumns();
            } else {
                removeSpecialColumns();
            }
        });

        showExtraFileColumnsProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                insertExtraFileColumns();
            } else {
                removeExtraFileColumns();
            }
        });
    }

    @Override
    public void setValues() {
        showFileColumnProperty.setValue(columnPreferences.showFileColumn());
        showUrlColumnProperty.setValue(columnPreferences.showUrlColumn());
        preferUrlProperty.setValue(!columnPreferences.preferDoiOverUrl());
        preferDoiProperty.setValue(columnPreferences.preferDoiOverUrl());
        showEPrintColumnProperty.setValue(columnPreferences.showEprintColumn());
        specialFieldsEnabledProperty.setValue(!columnPreferences.getSpecialFieldColumns().isEmpty());
        specialFieldsSyncKeyWordsProperty.setValue(columnPreferences.getAutoSyncSpecialFieldsToKeyWords());
        specialFieldsSerializeProperty.setValue(columnPreferences.getSerializeSpecialFields());
        showExtraFileColumnsProperty.setValue(!columnPreferences.getExtraFileColumns().isEmpty());

        fillColumnList();

        // Internal Fields
        List<Field> internalFields = new ArrayList<>();
        internalFields.add(InternalField.OWNER);
        internalFields.add(InternalField.TIMESTAMP);
        internalFields.add(InternalField.GROUPS);
        internalFields.add(InternalField.KEY_FIELD);
        internalFields.add(InternalField.TYPE_HEADER);
        internalFields.forEach(item -> selectableFieldProperty.getValue().add(0, item));

        // Special Fields
        if (specialFieldsEnabledProperty.getValue()) {
            insertSpecialColumns();
        }

        // Standard Fields
        EnumSet.allOf(StandardField.class).forEach(item -> selectableFieldProperty.getValue().add(0, item));

        // Extra File Columns
        if (showExtraFileColumnsProperty.getValue()) {
            insertExtraFileColumns();
        }
    }

    public void fillColumnList() {
        columnsListProperty.getValue().clear();

        List<Field> normalFields = columnPreferences.getNormalColumns().stream()
                .map(FieldFactory::parseField)
                .collect(Collectors.toList());

        normalFields.forEach(field -> columnsListProperty.getValue().add(
                new TableColumnsItemModel(
                        field,
                        columnPreferences.getPrefColumnWidth(field.getName())
                )));
    }

    private void insertSpecialColumns() {
        List<Field> fields = new ArrayList<>(EnumSet.allOf(SpecialField.class));
        fields.forEach(item -> selectableFieldProperty.getValue().add(0, item));
    }

    private void removeSpecialColumns() {
        List<TableColumnsItemModel> columns = columnsListProperty.getValue().stream()
                                                                 .filter(column -> (column.getField() instanceof SpecialField))
                                                                 .collect(Collectors.toList());
        columnsListProperty.getValue().removeAll(columns);

        List<Field> fields = selectableFieldProperty.getValue().stream()
                .filter(field -> (field instanceof SpecialField))
                .collect(Collectors.toList());

        selectableFieldProperty.getValue().removeAll(fields);
    }

    private void insertExtraFileColumns() {
        List<ExternalFileType> fileTypes = new ArrayList<>(ExternalFileTypes.getInstance().getExternalFileTypeSelection());
        List<Field> fileColumns = new ArrayList<>();
        fileTypes.stream().map(ExternalFileType::getName)
                .forEach(fileName -> fileColumns.add(new ExtraFileField(fileName)));

        fileColumns.forEach(item -> selectableFieldProperty.getValue().add(0, item));
    }

    private void removeExtraFileColumns() {
        List<TableColumnsItemModel> columns = columnsListProperty.getValue().stream()
                                                                 .filter(column -> (column.getField() instanceof ExtraFileField))
                                                                 .collect(Collectors.toList());
        columnsListProperty.getValue().removeAll(columns);

        List<Field> fields = selectableFieldProperty.getValue().stream()
                                                    .filter(field -> (field instanceof ExtraFileField))
                                                    .collect(Collectors.toList());
        selectableFieldProperty.getValue().removeAll(fields);
    }

    public void insertColumnInList() {
        if (addFieldProperty.getValue() == null) {
            return;
        }

        if (columnsListProperty.getValue().stream().filter(item -> item.getField().equals(addFieldProperty.getValue())).findAny().isEmpty()) {
            columnsListProperty.add(new TableColumnsItemModel(addFieldProperty.getValue()));
            addFieldProperty.setValue(null);
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
        List<String> normalColumns = columnsListProperty.stream().map(item -> item.getField().getName()).collect(Collectors.toList());
        List<SpecialField> specialFields = new ArrayList<>();
        List<String> deleteNames = new ArrayList<>();
        List<String> extraFileNames = new ArrayList<>();
        Map<String,Double> columnWidths = new TreeMap<>();

        normalColumns.forEach(fieldName -> {
                SpecialField.fromName(fieldName).ifPresent(field -> {
                    specialFields.add(field);
                    deleteNames.add(fieldName);
                });
                if (columnPreferences.getExtraFileColumns().contains(fieldName)) {
                    extraFileNames.add(fieldName);
                    deleteNames.add(fieldName);
                }
        });
        normalColumns.removeAll(deleteNames);

        // for each single one, to get stored or default value
        normalColumns.forEach(field -> columnWidths.put(field,columnPreferences.getPrefColumnWidth(field)));

        ColumnPreferences newColumnPreferences = new ColumnPreferences(
                showFileColumnProperty.getValue(),
                showUrlColumnProperty.getValue(),
                preferDoiProperty.getValue(),
                showEPrintColumnProperty.getValue(),
                normalColumns,
                specialFields,
                specialFieldsSyncKeyWordsProperty.getValue(),
                specialFieldsSerializeProperty.getValue(),
                extraFileNames,
                columnWidths,
                columnPreferences.getSortTypesForColumns()
        );

        if (!(columnPreferences.getSpecialFieldColumns().equals(newColumnPreferences.getSpecialFieldColumns()))) {
            restartWarnings.add(Localization.lang("Special field column"));
        }

        if (columnPreferences.getAutoSyncSpecialFieldsToKeyWords() != newColumnPreferences.getAutoSyncSpecialFieldsToKeyWords()) {
            restartWarnings.add(Localization.lang("Synchronize special fields to keywords"));
        }

        if (columnPreferences.getSerializeSpecialFields() != newColumnPreferences.getSerializeSpecialFields()) {
            restartWarnings.add(Localization.lang("Serialize special fields"));
        }

        preferences.storeColumnPreferences(newColumnPreferences);
    }

    @Override
    public boolean validateSettings() { return true; } // should contain at least one column

    @Override
    public List<String> getRestartWarnings() {
        return restartWarnings;
    }

    public ListProperty<TableColumnsItemModel> columnsListProperty() { return this.columnsListProperty; }

    public ObjectProperty<SelectionModel<TableColumnsItemModel>> selectedColumnModelProperty() { return selectedColumnModelProperty; }

    public ListProperty<Field> selectableFieldProperty() { return this.selectableFieldProperty; }

    public ObjectProperty<Field> addFieldProperty() { return this.addFieldProperty; }

    public BooleanProperty showFileColumnProperty() { return this.showFileColumnProperty; }

    public BooleanProperty showUrlColumnProperty() { return this.showUrlColumnProperty; }

    public BooleanProperty preferUrlProperty() { return this.preferUrlProperty; }

    public BooleanProperty preferDoiProperty() { return this.preferDoiProperty; }

    public BooleanProperty specialFieldsEnabledProperty() { return this.specialFieldsEnabledProperty; }

    public BooleanProperty specialFieldsSyncKeyWordsProperty() { return this.specialFieldsSyncKeyWordsProperty; }

    public BooleanProperty specialFieldsSerializeProperty() { return this.specialFieldsSerializeProperty; }

    public BooleanProperty showEPrintColumnProperty() { return this.showEPrintColumnProperty; }

    public BooleanProperty showExtraFileColumnsProperty() { return this.showExtraFileColumnsProperty; }

    class ExtraFileField implements Field {

        String name;

        ExtraFileField(String name) {
            this.name = name;
        }

        @Override
        public Set<FieldProperty> getProperties() {
            return Collections.emptySet();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isStandardField() {
            return false;
        }
    }
}
