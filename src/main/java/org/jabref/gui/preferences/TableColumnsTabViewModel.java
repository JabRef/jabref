package org.jabref.gui.preferences;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    // ToDo: Convert to CheckListView-CheckModel
    private final SimpleBooleanProperty specialFieldsEnabledProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldRankingProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldQualityProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldPriorityProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldRelevanceProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldPrintedProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldReadStatusProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldsSyncKeyWordsProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldsSerializeProperty = new SimpleBooleanProperty();

    private final SimpleBooleanProperty fileFieldProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty urlFieldEnabledProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty preferUrlProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty preferDoiProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty ePrintFieldProperty = new SimpleBooleanProperty();

    private final SimpleBooleanProperty extraFileFieldsEnabledProperty = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final ColumnPreferences columnPreferences;
    private final JabRefFrame frame;

    public TableColumnsTabViewModel(DialogService dialogService, JabRefPreferences preferences, JabRefFrame frame) {
        this.dialogService = dialogService;
        this.columnPreferences = preferences.getColumnPreferences();
        this.frame = frame;

        // identifierFieldsEnabledProperty.addListener((observable, oldValue, newValue) -> setValues());
        // extraFileFieldsEnabledProperty.addListener((observable, oldValue, newValue) -> setValues());

        specialFieldsEnabledProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                insertSpecialColumns();
            } else {
                removeSpecialColumns();
            }
            // ToDo: RestartMessage
        });

        extraFileFieldsEnabledProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                insertExtraFileColumns();
            } else {
                removeExtraFileColumns();
            }
            // ToDo: RestartMessage
        });
    }

    @Override
    public void setValues() {
        Set<TableColumnsItemModel> fields = new HashSet<>();

        columnsListProperty.getValue().clear();
        columnPreferences.getNormalColumns().forEach(
                item -> columnsListProperty.getValue().add(new TableColumnsItemModel(FieldFactory.parseField(item))));

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

        // ToDo: convert to CheckListView-CheckModel
        specialFieldRankingProperty.setValue(columnPreferences.getSpecialFieldColumns().contains(SpecialField.RANKING));
        specialFieldQualityProperty.setValue(columnPreferences.getSpecialFieldColumns().contains(SpecialField.QUALITY));
        specialFieldPriorityProperty.setValue(columnPreferences.getSpecialFieldColumns().contains(SpecialField.PRIORITY));
        specialFieldRelevanceProperty.setValue(columnPreferences.getSpecialFieldColumns().contains(SpecialField.RELEVANCE));
        specialFieldPrintedProperty.setValue(columnPreferences.getSpecialFieldColumns().contains(SpecialField.PRINTED));
        specialFieldReadStatusProperty.setValue(columnPreferences.getSpecialFieldColumns().contains(SpecialField.READ_STATUS));

        // HardCoded Fields
        fileFieldProperty.setValue(columnPreferences.showFileColumn());
        urlFieldEnabledProperty.setValue(columnPreferences.showUrlColumn());
        preferUrlProperty.setValue(!columnPreferences.preferDoiOverUrl());
        preferDoiProperty.setValue(columnPreferences.preferDoiOverUrl());
        ePrintFieldProperty.setValue(columnPreferences.showEprintColumn());
        extraFileFieldsEnabledProperty.setValue(!columnPreferences.getExtraFileColumns().isEmpty());
        if (extraFileFieldsEnabledProperty.getValue()) {
            insertExtraFileColumns();
        }

        insertColumns(EnumSet.allOf(StandardField.class).stream().map(TableColumnsItemModel::new).collect(Collectors.toSet()));
    }

    private void insertSpecialColumns() {
        Set<TableColumnsItemModel> fields = new HashSet<>();

        EnumSet.allOf(SpecialField.class).forEach(specialField -> {
            TableColumnsItemModel column = new TableColumnsItemModel(specialField);
            fields.add(column);

            /* if (specialField == SpecialField.RANKING && specialFieldRankingProperty.getValue()) {
                checkedColumnsModelProperty.getValue().check(column);
            } else if (specialField == SpecialField.QUALITY && specialFieldQualityProperty.getValue()) {
                checkedColumnsModelProperty.getValue().check(column);
            } else if (specialField == SpecialField.PRIORITY && specialFieldPriorityProperty.getValue()) {
                checkedColumnsModelProperty.getValue().check(column);
            } else if (specialField == SpecialField.RELEVANCE && specialFieldRelevanceProperty.getValue()) {
                checkedColumnsModelProperty.getValue().check(column);
            } else if (specialField == SpecialField.PRINTED && specialFieldPrintedProperty.getValue()) {
                checkedColumnsModelProperty.getValue().check(column);
            } else if (specialField == SpecialField.READ_STATUS && specialFieldReadStatusProperty.getValue()) {
                checkedColumnsModelProperty.getValue().check(column);
            } */
        });

        insertColumns(fields);
    }

    private void removeSpecialColumns() {
        List<TableColumnsItemModel> columns = columnsListProperty.getValue().stream()
                .filter(column -> (column.getField() instanceof SpecialField))
                .collect(Collectors.toList());

        columnsListProperty.getValue().removeAll(columns);
    }

    private void insertExtraFileColumns() {
        Set<ExternalFileType> fileTypes = ExternalFileTypes.getInstance().getExternalFileTypeSelection();
        Set<TableColumnsItemModel> fileColumns = new HashSet<>();
        fileTypes.stream().map(ExternalFileType::getName)
                .forEach(fileName -> fileColumns.add(new TableColumnsItemModel(new ExtraFileField(fileName))));

        insertColumns(fileColumns);
    }

    private void removeExtraFileColumns() {
        List<TableColumnsItemModel> columns = columnsListProperty.getValue().stream()
                .filter(column -> (column.getField() instanceof ExtraFileField))
                .collect(Collectors.toList());

        columnsListProperty.getValue().removeAll(columns);
    }

    private void insertColumns(Set<TableColumnsItemModel> fields) {
        fields.stream()
                .filter(field -> columnsListProperty.getValue().filtered(item -> item.getName().equals(field.getName())).isEmpty())
                .forEach(columnsListProperty.getValue()::add);
    }

    @Override
    public void storeSettings() {

    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    public ListProperty<TableColumnsItemModel> columnsListProperty() { return this.columnsListProperty; }

    public ObjectProperty<IndexedCheckModel<TableColumnsItemModel>> checkedColumnsModelProperty() { return this.checkedColumnsModelProperty; }

    public SimpleBooleanProperty fileFieldProperty() { return this.fileFieldProperty; }

    public SimpleBooleanProperty urlFieldEnabledProperty() { return this.urlFieldEnabledProperty; }

    public SimpleBooleanProperty preferUrlProperty() { return this.preferUrlProperty; }

    public SimpleBooleanProperty preferDoiProperty() { return this.preferDoiProperty; }

    public SimpleBooleanProperty specialFieldsEnabledProperty() { return this.specialFieldsEnabledProperty; }

    public SimpleBooleanProperty specialFieldsSyncKeyWordsProperty() { return this.specialFieldsSyncKeyWordsProperty; }

    public SimpleBooleanProperty specialFieldsSerializeProperty() { return this.specialFieldsSerializeProperty; }

    public SimpleBooleanProperty eprintFieldProperty() { return this.ePrintFieldProperty; }

    public SimpleBooleanProperty extraFieldsEnabledProperty() { return this.extraFileFieldsEnabledProperty; }

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
