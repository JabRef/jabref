package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

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
import org.jabref.gui.util.NoCheckModel;
import org.jabref.gui.util.NoSelectionModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.preferences.JabRefPreferences;

import org.controlsfx.control.IndexedCheckModel;

public class TableColumnsTabViewModel implements PreferenceTabViewModel {

    private final ListProperty<TableColumnsItemModel> columnsListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<IndexedCheckModel<TableColumnsItemModel>> checkedColumnsModelProperty = new SimpleObjectProperty<>(new NoCheckModel<>());
    private final ObjectProperty<SelectionModel<TableColumnsItemModel>> selectedColumnModelProperty = new SimpleObjectProperty<>(new NoSelectionModel<>());

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
    }

    public void fillColumnList() {
        columnsListProperty.getValue().clear();

        // Stored Fields
        List<Field> normalFields = columnPreferences.getNormalColumns().stream()
                .map(FieldFactory::parseField)
                .collect(Collectors.toList());

        normalFields.forEach(field -> columnsListProperty.getValue().add(
                new TableColumnsItemModel(
                        field,
                        columnPreferences.getPrefColumnWidth(field.getName())
                )));

        // Internal Fields
        List<TableColumnsItemModel> internalFields = new ArrayList<>();
        internalFields.add(new TableColumnsItemModel(InternalField.OWNER));
        internalFields.add(new TableColumnsItemModel(InternalField.TIMESTAMP));
        internalFields.add(new TableColumnsItemModel(InternalField.GROUPS));
        internalFields.add(new TableColumnsItemModel(InternalField.KEY_FIELD));
        internalFields.add(new TableColumnsItemModel(InternalField.TYPE_HEADER));
        insertColumns(internalFields);

        // Standard Fields
        insertColumns(EnumSet.allOf(StandardField.class).stream().map(TableColumnsItemModel::new).collect(Collectors.toList()));

        // Special Fields
        if (specialFieldsEnabledProperty.getValue()) {
            insertSpecialColumns();
        }

        // Extra File Columns
        if (showExtraFileColumnsProperty.getValue()) {
            insertExtraFileColumns();
        }

        // Checks
        List<Field> checks = new ArrayList<>(normalFields);
        if (specialFieldsEnabledProperty.getValue()) {
            List<SpecialField> specialFields = new ArrayList<>(columnPreferences.getSpecialFieldColumns());
            checks.add(specialFields.contains(SpecialField.QUALITY) ? SpecialField.QUALITY : null);
            checks.add(specialFields.contains(SpecialField.PRIORITY) ? SpecialField.PRIORITY : null);
            checks.add(specialFields.contains(SpecialField.RELEVANCE) ? SpecialField.RELEVANCE : null);
            checks.add(specialFields.contains(SpecialField.PRINTED) ? SpecialField.PRINTED : null);
            checks.add(specialFields.contains(SpecialField.READ_STATUS) ? SpecialField.READ_STATUS : null);
        }
        setChecks(checks);
    }

    private void insertSpecialColumns() {
        List<Field> backupChecks = getChecks();
        List<TableColumnsItemModel> fields = new ArrayList<>();
        EnumSet.allOf(SpecialField.class).forEach(specialField -> fields.add(new TableColumnsItemModel(specialField)));

        insertColumns(fields);
        setChecks(backupChecks);
    }

    private void removeSpecialColumns() {
        List<Field> backupChecks = getChecks();
        List<TableColumnsItemModel> columns = columnsListProperty.getValue().stream()
                .filter(column -> (column.getField() instanceof SpecialField))
                .collect(Collectors.toList());

        columnsListProperty.getValue().removeAll(columns);
        setChecks(backupChecks);
    }

    private void insertExtraFileColumns() {
        List<Field> backupChecks = getChecks();
        List<ExternalFileType> fileTypes = new ArrayList<>(ExternalFileTypes.getInstance().getExternalFileTypeSelection());
        List<TableColumnsItemModel> fileColumns = new ArrayList<>();
        fileTypes.stream().map(ExternalFileType::getName)
                .forEach(fileName -> fileColumns.add(new TableColumnsItemModel(new ExtraFileField(fileName))));

        insertColumns(fileColumns);
        setChecks(backupChecks);
    }

    private void removeExtraFileColumns() {
        List<Field> backupChecks = getChecks();
        List<TableColumnsItemModel> columns = columnsListProperty.getValue().stream()
                .filter(column -> (column.getField() instanceof ExtraFileField))
                .collect(Collectors.toList());

        columnsListProperty.getValue().removeAll(columns);
        setChecks(backupChecks);
    }

    public void insertCustomColumn() {
        Optional<String> columnName = dialogService.showInputDialogWithDefaultAndWait(
                Localization.lang("New custom column"),
                Localization.lang("Name") + ":",
                Localization.lang("untitled")
        );

        columnName.ifPresent(name -> {
            if (columnsListProperty.getValue().filtered(item -> item.getName().equals(name)).isEmpty()) {
                List<Field> backupChecks = getChecks();
                TableColumnsItemModel newItem = new TableColumnsItemModel(new UnknownField(name));
                columnsListProperty.add(0, newItem);
                setChecks(backupChecks);
            }
        });
    }

    public void removeCustomColumn(TableColumnsItemModel column) {
        if (column != null && (column.getField() instanceof UnknownField)) {
            List<Field> backupChecks = getChecks();
            columnsListProperty.remove(column);
            setChecks(backupChecks);
        }
    }

    private void insertColumns(List<TableColumnsItemModel> fields) {
        fields.stream()
                .filter(field -> columnsListProperty.getValue()
                        .filtered(item -> item.getName().equals(field.getName())).isEmpty())
                .forEach(columnsListProperty.getValue()::add);
    }

    public List<Field> getChecks() {
        return checkedColumnsModelProperty.getValue().getCheckedItems().stream()
                .map(TableColumnsItemModel::getField)
                .collect(Collectors.toList());
    }

    public void setChecks(List<Field> fields) {
        columnsListProperty.stream().filter(item -> fields.contains(item.getField()))
                .forEach(item -> checkedColumnsModelProperty.getValue().check(item));
    }

    public void moveColumnUp() {

        TableColumnsItemModel selectedColumn = selectedColumnModelProperty.getValue().getSelectedItem();
        int row = columnsListProperty.getValue().indexOf(selectedColumn);
        if (selectedColumn == null || row < 1) {
            return;
        }

        List<Field> backupChecks = getChecks();
        columnsListProperty.remove(selectedColumn);
        columnsListProperty.add(row - 1, selectedColumn);
        selectedColumnModelProperty.getValue().clearAndSelect(row - 1);
        setChecks(backupChecks);
    }

    public void moveColumnDown() {
        TableColumnsItemModel selectedColumn = selectedColumnModelProperty.getValue().getSelectedItem();
        int row = columnsListProperty.getValue().indexOf(selectedColumn);
        if (selectedColumn == null || row > columnsListProperty.getValue().size() - 1) {
            return;
        }

        List<Field> backupChecks = getChecks();
        columnsListProperty.remove(selectedColumn);
        columnsListProperty.add(row + 1, selectedColumn);
        selectedColumnModelProperty.getValue().clearAndSelect(row + 1);
        setChecks(backupChecks);
    }

    @Override
    public void storeSettings() {
        List<String> normalColumns = getChecks().stream().map(Field::getName).collect(Collectors.toList());
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

    public ObjectProperty<IndexedCheckModel<TableColumnsItemModel>> checkedColumnsModelProperty() {
        return this.checkedColumnsModelProperty;
    }

    public ObjectProperty<SelectionModel<TableColumnsItemModel>> selectedColumnModelProperty() {
        return selectedColumnModelProperty;
    }

    public SimpleBooleanProperty showFileColumnProperty() { return this.showFileColumnProperty; }

    public SimpleBooleanProperty showUrlColumnProperty() { return this.showUrlColumnProperty; }

    public SimpleBooleanProperty preferUrlProperty() { return this.preferUrlProperty; }

    public SimpleBooleanProperty preferDoiProperty() { return this.preferDoiProperty; }

    public SimpleBooleanProperty specialFieldsEnabledProperty() { return this.specialFieldsEnabledProperty; }

    public SimpleBooleanProperty specialFieldsSyncKeyWordsProperty() { return this.specialFieldsSyncKeyWordsProperty; }

    public SimpleBooleanProperty specialFieldsSerializeProperty() { return this.specialFieldsSerializeProperty; }

    public SimpleBooleanProperty showEPrintColumnProperty() { return this.showEPrintColumnProperty; }

    public SimpleBooleanProperty showExtraFileColumnsProperty() { return this.showExtraFileColumnsProperty; }

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
