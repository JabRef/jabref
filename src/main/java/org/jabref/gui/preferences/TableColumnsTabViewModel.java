package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.SelectionModel;
import javafx.util.StringConverter;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.maintable.MainTablePreferences;
import org.jabref.gui.specialfields.SpecialFieldsPreferences;
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
import de.saxsys.mvvmfx.utils.validation.Validator;

public class TableColumnsTabViewModel implements PreferenceTabViewModel {

    static StringConverter<MainTableColumnModel> columnNameStringConverter = new StringConverter<>() {
        @Override
        public String toString(MainTableColumnModel object) {
            if (object != null) {
                return object.getName();
            } else {
                return "";
            }
        }

        @Override
        public MainTableColumnModel fromString(String string) {
            return MainTableColumnModel.parse(string);
        }
    };

    private final ListProperty<MainTableColumnModel> columnsListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<SelectionModel<MainTableColumnModel>> selectedColumnModelProperty = new SimpleObjectProperty<>(new NoSelectionModel<>());
    private final ListProperty<MainTableColumnModel> availableColumnsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<MainTableColumnModel> addColumnProperty = new SimpleObjectProperty<>();
    private final BooleanProperty specialFieldsEnabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty specialFieldsSyncKeywordsProperty = new SimpleBooleanProperty();
    private final BooleanProperty specialFieldsSerializeProperty = new SimpleBooleanProperty();
    private final BooleanProperty extraFileColumnsEnabledProperty = new SimpleBooleanProperty();

    private Validator columnsNotEmptyValidator;

    private List<String> restartWarnings = new ArrayList<>();

    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final MainTablePreferences mainTablePreferences;
    private final ColumnPreferences columnPreferences;
    private final SpecialFieldsPreferences specialFieldsPreferences;

    public TableColumnsTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.mainTablePreferences = preferences.getMainTablePreferences();
        this.columnPreferences = mainTablePreferences.getColumnPreferences();
        this.specialFieldsPreferences = preferences.getSpecialFieldsPreferences();

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
        specialFieldsEnabledProperty.setValue(specialFieldsPreferences.getSpecialFieldsEnabled());
        specialFieldsSyncKeywordsProperty.setValue(specialFieldsPreferences.getAutoSyncSpecialFieldsToKeyWords());
        specialFieldsSerializeProperty.setValue(specialFieldsPreferences.getSerializeSpecialFields());
        extraFileColumnsEnabledProperty.setValue(mainTablePreferences.getExtraFileColumnsEnabled());

        fillColumnList();

        availableColumnsProperty.clear();

        availableColumnsProperty.addAll(
                new MainTableColumnModel(MainTableColumnModel.Type.INDEX),
                new MainTableColumnModel(MainTableColumnModel.Type.LINKED_IDENTIFIER),
                new MainTableColumnModel(MainTableColumnModel.Type.GROUPS),
                new MainTableColumnModel(MainTableColumnModel.Type.FILES),
                new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, InternalField.TIMESTAMP.getName()),
                new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, InternalField.OWNER.getName()),
                new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, InternalField.GROUPS.getName()),
                new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, InternalField.KEY_FIELD.getName()),
                new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, InternalField.TYPE_HEADER.getName())
        );

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
        columnPreferences.getColumns().forEach(columnsListProperty.getValue()::add);
    }

    private void insertSpecialFieldColumns() {
        EnumSet.allOf(SpecialField.class).stream()
               .map(Field::getName)
               .map(name -> new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, name))
               .forEach(item -> availableColumnsProperty.getValue().add(0, item));
    }

    private void removeSpecialFieldColumns() {
        columnsListProperty.getValue().removeIf(column -> column.getType().equals(MainTableColumnModel.Type.SPECIALFIELD));
        availableColumnsProperty.getValue().removeIf(column -> column.getType().equals(MainTableColumnModel.Type.SPECIALFIELD));
    }

    private void insertExtraFileColumns() {
        ExternalFileTypes.getInstance().getExternalFileTypeSelection().stream()
                         .map(ExternalFileType::getName)
                         .map(name -> new MainTableColumnModel(MainTableColumnModel.Type.EXTRAFILE, name))
                         .forEach(item -> availableColumnsProperty.getValue().add(item));
    }

    private void removeExtraFileColumns() {
        columnsListProperty.getValue().removeIf(column -> column.getType().equals(MainTableColumnModel.Type.EXTRAFILE));
        availableColumnsProperty.getValue().removeIf(column -> column.getType().equals(MainTableColumnModel.Type.EXTRAFILE));
    }

    public void insertColumnInList() {
        if (addColumnProperty.getValue() == null) {
            return;
        }

        if (columnsListProperty.getValue().stream().filter(item -> item.equals(addColumnProperty.getValue())).findAny().isEmpty()) {
            columnsListProperty.add(addColumnProperty.getValue());
            addColumnProperty.setValue(null);
        }
    }

    public void removeColumn(MainTableColumnModel column) {
        columnsListProperty.remove(column);
    }

    public void moveColumnUp() {
        MainTableColumnModel selectedColumn = selectedColumnModelProperty.getValue().getSelectedItem();
        int row = columnsListProperty.getValue().indexOf(selectedColumn);
        if (selectedColumn == null || row < 1) {
            return;
        }

        columnsListProperty.remove(selectedColumn);
        columnsListProperty.add(row - 1, selectedColumn);
        selectedColumnModelProperty.getValue().clearAndSelect(row - 1);
    }

    public void moveColumnDown() {
        MainTableColumnModel selectedColumn = selectedColumnModelProperty.getValue().getSelectedItem();
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
        MainTablePreferences newMainTablePreferences = preferences.getMainTablePreferences();
        preferences.storeMainTablePreferences(new MainTablePreferences(
                new ColumnPreferences(
                        columnsListProperty.getValue(),
                        newMainTablePreferences.getColumnPreferences().getColumnSortOrder()),
                newMainTablePreferences.getResizeColumnsToFit(),
                extraFileColumnsEnabledProperty.getValue()
        ));

        SpecialFieldsPreferences newSpecialFieldsPreferences = new SpecialFieldsPreferences(
                specialFieldsEnabledProperty.getValue(),
                specialFieldsSyncKeywordsProperty.getValue(),
                specialFieldsSerializeProperty.getValue());

        if (specialFieldsPreferences.getAutoSyncSpecialFieldsToKeyWords() != newSpecialFieldsPreferences.getAutoSyncSpecialFieldsToKeyWords()) {
            restartWarnings.add(Localization.lang("Synchronize special fields to keywords"));
        }

        if (specialFieldsPreferences.getSerializeSpecialFields() != newSpecialFieldsPreferences.getSerializeSpecialFields()) {
            restartWarnings.add(Localization.lang("Serialize special fields"));
        }

        preferences.storeSpecialFieldsPreferences(newSpecialFieldsPreferences);
    }

    ValidationStatus columnsListValidationStatus() {
        return columnsNotEmptyValidator.getValidationStatus();
    }

    @Override
    public boolean validateSettings() {
        ValidationStatus validationStatus = columnsListValidationStatus();
        if (!validationStatus.isValid()) {
            validationStatus.getHighestMessage().ifPresent(message ->
                    dialogService.showErrorDialogAndWait(message.getMessage()));
            return false;
        }
        return true;
    }

    @Override
    public List<String> getRestartWarnings() {
        return restartWarnings;
    }

    public ListProperty<MainTableColumnModel> columnsListProperty() { return this.columnsListProperty; }

    public ObjectProperty<SelectionModel<MainTableColumnModel>> selectedColumnModelProperty() { return selectedColumnModelProperty; }

    public ListProperty<MainTableColumnModel> availableColumnsProperty() { return this.availableColumnsProperty; }

    public ObjectProperty<MainTableColumnModel> addColumnProperty() { return this.addColumnProperty; }

    public BooleanProperty specialFieldsEnabledProperty() { return this.specialFieldsEnabledProperty; }

    public BooleanProperty specialFieldsSyncKeywordsProperty() { return this.specialFieldsSyncKeywordsProperty; }

    public BooleanProperty specialFieldsSerializeProperty() { return this.specialFieldsSerializeProperty; }

    public BooleanProperty extraFileColumnsEnabledProperty() { return this.extraFileColumnsEnabledProperty; }

}
