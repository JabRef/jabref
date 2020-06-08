package org.jabref.gui.customentrytypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.UnknownEntryType;
import org.jabref.preferences.PreferencesService;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class CustomEntryTypeDialogViewModel {

    public static final StringConverter<Field> FIELD_STRING_CONVERTER = new StringConverter<>() {

        @Override
        public String toString(Field object) {
            return object != null ? object.getDisplayName() : "";
        }

        @Override
        public Field fromString(String string) {
            return new UnknownField(string);
        }
    };

    private final ObservableList<Field> fieldsForAdding;
    private final ObjectProperty<CustomEntryTypeViewModel> selectedEntryTypes = new SimpleObjectProperty<>();
    private final ObjectProperty<Field> selectedFieldToAdd = new SimpleObjectProperty<>();
    private final StringProperty entryTypeToAdd = new SimpleStringProperty("");
    private final ObservableList<BibEntryType> allEntryTypes;
    private final ObjectProperty<Field> newFieldToAdd = new SimpleObjectProperty<>();
    private final BibDatabaseMode mode;
    private final ObservableList<CustomEntryTypeViewModel> entryTypesWithFields = FXCollections.observableArrayList(extractor -> new Observable[] {extractor.entryType(), extractor.fields()});

    private final PreferencesService preferencesService;
    private final BibEntryTypesManager entryTypesManager;

    private final Validator entryTypeValidator;
    private final Validator fieldValidator;

    public CustomEntryTypeDialogViewModel(BibDatabaseMode mode, PreferencesService preferencesService, BibEntryTypesManager entryTypesManager) {
        this.mode = mode;
        this.preferencesService = preferencesService;
        this.entryTypesManager = entryTypesManager;

        Collection<BibEntryType> allTypes = entryTypesManager.getAllTypes(mode);
        allTypes.addAll(entryTypesManager.getAllCustomTypes(mode));

        allEntryTypes = FXCollections.observableArrayList(allTypes);
        fieldsForAdding = new SimpleListProperty<>(FXCollections.observableArrayList(FieldFactory.getStandardFielsdsWithBibTexKey()));

        for (BibEntryType entryType : allTypes) {
            CustomEntryTypeViewModel viewModel = new CustomEntryTypeViewModel(entryType);
            this.entryTypesWithFields.add(viewModel);
        }

        Predicate<String> notEmpty = input -> (input != null) && !input.trim().isEmpty();
        entryTypeValidator = new FunctionBasedValidator<>(entryTypeToAdd, notEmpty, ValidationMessage.error(Localization.lang("Entry type cannot be empty. Please enter a name.")));
        fieldValidator = new FunctionBasedValidator<>(newFieldToAdd,
                                                      input -> (input != null) && !input.getDisplayName().isEmpty(),
                                                      ValidationMessage.error(Localization.lang("Field cannot be empty. Please enter a name.")));
    }

    public ObservableList<CustomEntryTypeViewModel> entryTypes() {
        return this.entryTypesWithFields;
    }

    public ObservableList<Field> fields() {
        return this.fieldsForAdding;
    }

    public ObservableList<FieldViewModel> fieldsForSelectedType() {
        return this.selectedEntryTypes.getValue().fields();
    }

    public enum FieldType {

        REQUIRED(Localization.lang("Required")),
        OPTIONAL(Localization.lang("Optional"));

        private final String name;

        FieldType(String name) {
            this.name = name;
        }

        public String getDisplayName() {
            return this.name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public void addNewField() {
        Field field = newFieldToAdd.getValue();
        FieldViewModel model = new FieldViewModel(field, true, FieldPriority.IMPORTANT);
        this.selectedEntryTypes.getValue().addField(model);
        newFieldToAddProperty().setValue(null);
    }

    public void addNewCustomEntryType() {
        EntryType newentryType = new UnknownEntryType(entryTypeToAdd.getValue());
        BibEntryType type = new BibEntryType(newentryType, new ArrayList<>(), Collections.emptyList());
        CustomEntryTypeViewModel viewModel = new CustomEntryTypeViewModel(type);
        this.entryTypesWithFields.add(viewModel);
        this.allEntryTypes.add(type);
        this.entryTypeToAdd.setValue("");
    }

    public ObjectProperty<CustomEntryTypeViewModel> selectedEntryTypeProperty() {
        return this.selectedEntryTypes;
    }

    public ObjectProperty<Field> selectedFieldToAddProperty() {
        return this.selectedFieldToAdd;
    }

    public StringProperty entryTypeToAddProperty() {
        return this.entryTypeToAdd;
    }

    public ObjectProperty<Field> newFieldToAddProperty() {
        return this.newFieldToAdd;
    }

    public ValidationStatus entryTypeValidationStatus() {
        return entryTypeValidator.getValidationStatus();
    }

    public ValidationStatus fieldValidationStatus() {
        return fieldValidator.getValidationStatus();
    }

    public void removeEntryType(CustomEntryTypeViewModel focusedItem) {
        entryTypesWithFields.remove(focusedItem);
    }

    public void removeField(FieldViewModel focusedItem) {
        this.selectedEntryTypes.getValue().removeField(focusedItem);
    }

    public void apply() {

        for (var typeWithField : entryTypesWithFields) {
            BibEntryType type = typeWithField.entryType().getValue();
            List<FieldViewModel> allFields = typeWithField.fields();

            List<OrFields> requiredFields = allFields.stream().filter(field -> field.getFieldType() == FieldType.REQUIRED).map(FieldViewModel::getField).map(OrFields::new).collect(Collectors.toList());
            List<BibField> otherFields = allFields.stream().filter(field -> field.getFieldType() == FieldType.OPTIONAL).map(bibField -> new BibField(bibField.getField(), bibField.getFieldPriority())).collect(Collectors.toList());

            BibEntryType newType = new BibEntryType(type.getType(), otherFields, requiredFields);
            entryTypesManager.addCustomOrModifiedType(newType, mode);
        }

        preferencesService.saveCustomEntryTypes();
        // Reload types from preferences to make sure any modifications are present when reopening the dialog
        entryTypesManager.addCustomOrModifiedTypes(preferencesService.loadBibEntryTypes(BibDatabaseMode.BIBTEX),
                                                   preferencesService.loadBibEntryTypes(BibDatabaseMode.BIBLATEX));
    }
}
