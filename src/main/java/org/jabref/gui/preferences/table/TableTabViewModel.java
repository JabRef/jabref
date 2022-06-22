package org.jabref.gui.preferences.table;

import java.util.EnumSet;

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
import org.jabref.gui.maintable.MainTableNameFormatPreferences;
import org.jabref.gui.maintable.MainTableNameFormatPreferences.AbbreviationStyle;
import org.jabref.gui.maintable.MainTableNameFormatPreferences.DisplayStyle;
import org.jabref.gui.maintable.MainTablePreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.specialfields.SpecialFieldsPreferences;
import org.jabref.gui.util.NoSelectionModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class TableTabViewModel implements PreferenceTabViewModel {

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
    private final BooleanProperty extraFileColumnsEnabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoResizeColumnsProperty = new SimpleBooleanProperty();

    private final BooleanProperty namesNatbibProperty = new SimpleBooleanProperty();
    private final BooleanProperty nameAsIsProperty = new SimpleBooleanProperty();
    private final BooleanProperty nameFirstLastProperty = new SimpleBooleanProperty();
    private final BooleanProperty nameLastFirstProperty = new SimpleBooleanProperty();
    private final BooleanProperty abbreviationDisabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty abbreviationEnabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty abbreviationLastNameOnlyProperty = new SimpleBooleanProperty();

    private final Validator columnsNotEmptyValidator;

    private final DialogService dialogService;
    private final PreferencesService preferences;

    private ColumnPreferences initialColumnPreferences;
    private final SpecialFieldsPreferences specialFieldsPreferences;

    public TableTabViewModel(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
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
        MainTablePreferences initialMainTablePreferences = preferences.getMainTablePreferences();
        initialColumnPreferences = initialMainTablePreferences.getColumnPreferences();
        MainTableNameFormatPreferences initialNameFormatPreferences = preferences.getMainTableNameFormatPreferences();

        specialFieldsEnabledProperty.setValue(specialFieldsPreferences.isSpecialFieldsEnabled());
        extraFileColumnsEnabledProperty.setValue(initialMainTablePreferences.getExtraFileColumnsEnabled());
        autoResizeColumnsProperty.setValue(initialMainTablePreferences.getResizeColumnsToFit());

        fillColumnList();

        availableColumnsProperty.clear();
        availableColumnsProperty.addAll(
                new MainTableColumnModel(MainTableColumnModel.Type.INDEX),
                new MainTableColumnModel(MainTableColumnModel.Type.LINKED_IDENTIFIER),
                new MainTableColumnModel(MainTableColumnModel.Type.GROUPS),
                new MainTableColumnModel(MainTableColumnModel.Type.FILES),
                new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, StandardField.TIMESTAMP.getName()),
                new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, StandardField.OWNER.getName()),
                new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, StandardField.GROUPS.getName()),
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

        switch (initialNameFormatPreferences.getDisplayStyle()) {
            case NATBIB -> namesNatbibProperty.setValue(true);
            case AS_IS -> nameAsIsProperty.setValue(true);
            case FIRSTNAME_LASTNAME -> nameFirstLastProperty.setValue(true);
            case LASTNAME_FIRSTNAME -> nameLastFirstProperty.setValue(true);
        }

        switch (initialNameFormatPreferences.getAbbreviationStyle()) {
            case FULL -> abbreviationEnabledProperty.setValue(true);
            case LASTNAME_ONLY -> abbreviationLastNameOnlyProperty.setValue(true);
            case NONE -> abbreviationDisabledProperty.setValue(true);
        }
    }

    public void fillColumnList() {
        columnsListProperty.getValue().clear();
        if (initialColumnPreferences != null) {
            initialColumnPreferences.getColumns().forEach(columnsListProperty.getValue()::add);
        }
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
        if ((selectedColumn == null) || (row < 1)) {
            return;
        }

        columnsListProperty.remove(selectedColumn);
        columnsListProperty.add(row - 1, selectedColumn);
        selectedColumnModelProperty.getValue().clearAndSelect(row - 1);
    }

    public void moveColumnDown() {
        MainTableColumnModel selectedColumn = selectedColumnModelProperty.getValue().getSelectedItem();
        int row = columnsListProperty.getValue().indexOf(selectedColumn);
        if ((selectedColumn == null) || (row > (columnsListProperty.getValue().size() - 2))) {
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
                autoResizeColumnsProperty.getValue(),
                extraFileColumnsEnabledProperty.getValue()
        ));

        specialFieldsPreferences.setSpecialFieldsEnabled(specialFieldsEnabledProperty.getValue());

        DisplayStyle displayStyle = DisplayStyle.LASTNAME_FIRSTNAME;
        if (namesNatbibProperty.getValue()) {
            displayStyle = DisplayStyle.NATBIB;
        } else if (nameAsIsProperty.getValue()) {
            displayStyle = DisplayStyle.AS_IS;
        } else if (nameFirstLastProperty.getValue()) {
            displayStyle = DisplayStyle.FIRSTNAME_LASTNAME;
        }

        AbbreviationStyle abbreviationStyle = AbbreviationStyle.NONE;
        if (abbreviationEnabledProperty.getValue()) {
            abbreviationStyle = AbbreviationStyle.FULL;
        } else if (abbreviationLastNameOnlyProperty.getValue()) {
            abbreviationStyle = AbbreviationStyle.LASTNAME_ONLY;
        }

        preferences.storeMainTableNameFormatPreferences(new MainTableNameFormatPreferences(displayStyle, abbreviationStyle));
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

    public ListProperty<MainTableColumnModel> columnsListProperty() {
        return this.columnsListProperty;
    }

    public ObjectProperty<SelectionModel<MainTableColumnModel>> selectedColumnModelProperty() {
        return selectedColumnModelProperty;
    }

    public ListProperty<MainTableColumnModel> availableColumnsProperty() {
        return this.availableColumnsProperty;
    }

    public ObjectProperty<MainTableColumnModel> addColumnProperty() {
        return this.addColumnProperty;
    }

    public BooleanProperty specialFieldsEnabledProperty() {
        return this.specialFieldsEnabledProperty;
    }

    public BooleanProperty extraFileColumnsEnabledProperty() {
        return this.extraFileColumnsEnabledProperty;
    }

    public BooleanProperty autoResizeColumnsProperty() {
        return autoResizeColumnsProperty;
    }

    public BooleanProperty namesNatbibProperty() {
        return namesNatbibProperty;
    }

    public BooleanProperty nameAsIsProperty() {
        return nameAsIsProperty;
    }

    public BooleanProperty nameFirstLastProperty() {
        return nameFirstLastProperty;
    }

    public BooleanProperty nameLastFirstProperty() {
        return nameLastFirstProperty;
    }

    public BooleanProperty abbreviationDisabledProperty() {
        return abbreviationDisabledProperty;
    }

    public BooleanProperty abbreviationEnabledProperty() {
        return abbreviationEnabledProperty;
    }

    public BooleanProperty abbreviationLastNameOnlyProperty() {
        return abbreviationLastNameOnlyProperty;
    }
}
