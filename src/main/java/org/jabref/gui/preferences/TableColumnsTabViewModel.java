package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.util.NoCheckModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.JabRefPreferences;

import org.controlsfx.control.IndexedCheckModel;

public class TableColumnsTabViewModel implements PreferenceTabViewModel {

    private final ListProperty<TableColumnsItemModel> columnsListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<IndexedCheckModel<TableColumnsItemModel>> checkedColumnsModelProperty = new SimpleObjectProperty<>(new NoCheckModel<>());

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
        Set<TableColumnsItemModel> fields = new HashSet<>();

        columnsListProperty.getValue().clear();

        columnPreferences.getNormalColumns().forEach(
                item -> columnsListProperty.getValue().add(
                        new TableColumnsItemModel(
                                FieldFactory.parseField(item),
                                columnPreferences.getPrefColumnWidth(item)
                        )));

        // Internal Fields
        fields.add(new TableColumnsItemModel(InternalField.OWNER));
        fields.add(new TableColumnsItemModel(InternalField.TIMESTAMP));
        fields.add(new TableColumnsItemModel(InternalField.GROUPS));
        fields.add(new TableColumnsItemModel(InternalField.KEY_FIELD));
        fields.add(new TableColumnsItemModel(InternalField.TYPE_HEADER));

        insertColumns(fields);

        // Special Fields
        specialFieldsEnabledProperty.setValue(!columnPreferences.getSpecialFieldColumns().isEmpty());
        specialFieldsSyncKeyWordsProperty.setValue(columnPreferences.getAutoSyncSpecialFieldsToKeyWords());
        specialFieldsSerializeProperty.setValue(columnPreferences.getSerializeSpecialFields());
        if (specialFieldsEnabledProperty.getValue()) {
            insertSpecialColumns();
        }

        // HardCoded Fields
        showFileColumnProperty.setValue(columnPreferences.showFileColumn());
        showUrlColumnProperty.setValue(columnPreferences.showUrlColumn());
        preferUrlProperty.setValue(!columnPreferences.preferDoiOverUrl());
        preferDoiProperty.setValue(columnPreferences.preferDoiOverUrl());
        showEPrintColumnProperty.setValue(columnPreferences.showEprintColumn());
        showExtraFileColumnsProperty.setValue(!columnPreferences.getExtraFileColumns().isEmpty());
        if (showExtraFileColumnsProperty.getValue()) {
            insertExtraFileColumns();
        }

        insertColumns(EnumSet.allOf(StandardField.class).stream().map(TableColumnsItemModel::new).collect(Collectors.toSet()));

        // Checks
        List<String> checks = new ArrayList<>(columnPreferences.getNormalColumns());
        if (specialFieldsEnabledProperty.getValue()) {
            List<SpecialField> specialFields = new ArrayList<>(columnPreferences.getSpecialFieldColumns());
            checks.add(specialFields.contains(SpecialField.QUALITY) ? SpecialField.QUALITY.getName() : null);
            checks.add(specialFields.contains(SpecialField.PRIORITY) ? SpecialField.PRIORITY.getName() : null);
            checks.add(specialFields.contains(SpecialField.RELEVANCE) ? SpecialField.RELEVANCE.getName() : null);
            checks.add(specialFields.contains(SpecialField.PRINTED) ? SpecialField.PRINTED.getName() : null);
            checks.add(specialFields.contains(SpecialField.READ_STATUS) ? SpecialField.READ_STATUS.getName() : null);
        }
        setChecks(checks);
    }

    private void insertSpecialColumns() {
        List<String> backupChecks = getChecks();
        Set<TableColumnsItemModel> fields = new HashSet<>();
        EnumSet.allOf(SpecialField.class)
                .forEach(specialField -> fields.add(new TableColumnsItemModel(specialField)));

        insertColumns(fields);
        setChecks(backupChecks);
    }

    private void removeSpecialColumns() {
        List<String> backupChecks = getChecks();
        List<TableColumnsItemModel> columns = columnsListProperty.getValue().stream()
                .filter(column -> (column.getField() instanceof SpecialField))
                .collect(Collectors.toList());

        columnsListProperty.getValue().removeAll(columns);
        setChecks(backupChecks);
    }

    private void insertExtraFileColumns() {
        List<String> backupChecks = getChecks();
        Set<ExternalFileType> fileTypes = ExternalFileTypes.getInstance().getExternalFileTypeSelection();
        Set<TableColumnsItemModel> fileColumns = new HashSet<>();
        fileTypes.stream().map(ExternalFileType::getName)
                .forEach(fileName -> fileColumns.add(new TableColumnsItemModel(new ExtraFileField(fileName))));

        insertColumns(fileColumns);
        setChecks(backupChecks);
    }

    private void removeExtraFileColumns() {
        List<String> backupChecks = getChecks();
        List<TableColumnsItemModel> columns = columnsListProperty.getValue().stream()
                .filter(column -> (column.getField() instanceof ExtraFileField))
                .collect(Collectors.toList());

        columnsListProperty.getValue().removeAll(columns);
        setChecks(backupChecks);
    }

    private void insertColumns(Set<TableColumnsItemModel> fields) {
        fields.stream()
                .filter(field -> columnsListProperty.getValue().filtered(item -> item.getName().equals(field.getName())).isEmpty())
                .forEach(columnsListProperty.getValue()::add);
    }

    public List<String> getChecks() {
        return checkedColumnsModelProperty.getValue().getCheckedItems().stream()
                .map(item -> item.getField().getName())
                .collect(Collectors.toList());
    }

    public void setChecks(List<String> fieldNames) {
        columnsListProperty.stream().filter(item -> fieldNames.contains(item.getField().getName()))
                .forEach(item -> checkedColumnsModelProperty.getValue().check(item));
    }

    @Override
    public void storeSettings() {
        List<String> normalColumns = getChecks();
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

    public ObjectProperty<IndexedCheckModel<TableColumnsItemModel>> checkedColumnsModelProperty() { return this.checkedColumnsModelProperty; }

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
