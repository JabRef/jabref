package org.jabref.gui.customentrytypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.Observable;
import javafx.beans.property.ListProperty;
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
import org.fxmisc.easybind.EasyBind;

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

    private final ListProperty<BibEntryType> entryTypes;
    private final ListProperty<Field> fields;
    private final ObjectProperty<BibEntryType> selectedEntryTypes = new SimpleObjectProperty<>();
    private final ListProperty<FieldViewModel> fieldsForType;
    private final ObjectProperty<Field> selectedFieldToAdd = new SimpleObjectProperty<>();
    private final StringProperty entryTypeToAdd = new SimpleStringProperty("");
    private final ObservableList<BibEntryType> allEntryTypes;
    private final ObservableList<FieldViewModel> allFieldsForType = FXCollections.observableArrayList(extractor -> new Observable[] {extractor.fieldName(), extractor.fieldType()});
    private final ObjectProperty<Field> newFieldToAdd = new SimpleObjectProperty<>();
    private final BibDatabaseMode mode;
    private final Map<BibEntryType, List<FieldViewModel>> typesWithFields = new HashMap<>();
    private final List<BibEntryType> typesToRemove = new ArrayList<>();

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
        entryTypes = new SimpleListProperty<>(allEntryTypes);

        fields = new SimpleListProperty<>(FXCollections.observableArrayList(FieldFactory.getCommonFields()));

        for (BibEntryType entryType : allTypes) {
            List<FieldViewModel> fields = entryType.getAllFields().stream().map(bibField -> new FieldViewModel(bibField.getField(), entryType.isRequired(bibField.getField()), bibField.getPriority(), entryType)).collect(Collectors.toList());
            typesWithFields.put(entryType, fields);
        }

        this.fieldsForType = new SimpleListProperty<>(allFieldsForType);

        EasyBind.subscribe(selectedEntryTypes, type -> {
            if (type != null) {
                allFieldsForType.setAll(typesWithFields.get(type));
            }
        });

        Predicate<String> notEmpty = input -> (input != null) && !input.trim().isEmpty();
        entryTypeValidator = new FunctionBasedValidator<>(entryTypeToAdd, notEmpty, ValidationMessage.error(Localization.lang("Entry type cannot be empty. Please enter a name.")));
        fieldValidator = new FunctionBasedValidator<>(newFieldToAdd,
                                                      input -> input != null && !input.getDisplayName().isEmpty(),
                                                      ValidationMessage.error(Localization.lang("Field cannot be empty. Please enter a name.")));
    }

    public ListProperty<BibEntryType> entryTypes() {
        return this.entryTypes;
    }

    public ListProperty<Field> fields() {
        return this.fields;
    }

    public enum FieldType {

        REQUIRED(Localization.lang("Required")),
        OPTIONAL(Localization.lang("Optional"));

        private String name;

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
        FieldViewModel model = new FieldViewModel(field, true, FieldPriority.IMPORTANT, selectedEntryTypes.getValue());
        typesWithFields.computeIfAbsent(selectedEntryTypes.getValue(), key -> new ArrayList<>()).add(model);
        allFieldsForType.add(model);
        newFieldToAddProperty().setValue(null);
    }

    public void addNewCustomEntryType() {
        EntryType newentryType = new UnknownEntryType(entryTypeToAdd.getValue());
        BibEntryType type = new BibEntryType(newentryType, new ArrayList<>(), Collections.emptyList());
        this.allEntryTypes.add(type);
        this.entryTypeToAdd.setValue("");
        this.typesWithFields.put(type, new ArrayList<>());
    }

    public ObjectProperty<BibEntryType> selectedEntryTypeProperty() {
        return this.selectedEntryTypes;
    }

    public ListProperty<FieldViewModel> fieldsforTypesProperty() {
        return this.fieldsForType;
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

    public void removeEntryType(BibEntryType focusedItem) {
        typesToRemove.add(focusedItem);
        typesWithFields.remove(focusedItem);
        allEntryTypes.remove(focusedItem);
    }

    public void removeField(FieldViewModel focusedItem) {
        typesWithFields.computeIfAbsent(selectedEntryTypes.getValue(), key -> new ArrayList<>()).remove(focusedItem);
        allFieldsForType.remove(focusedItem);
    }

    public void apply() {

        for (var typeWithField : typesWithFields.entrySet()) {
            BibEntryType type = typeWithField.getKey();
            List<FieldViewModel> allFields = typeWithField.getValue();

            List<OrFields> requiredFields = allFields.stream().filter(field -> field.getFieldType() == FieldType.REQUIRED).map(FieldViewModel::getField).map(OrFields::new).collect(Collectors.toList());
            List<BibField> otherFields = allFields.stream().filter(field -> field.getFieldType() == FieldType.OPTIONAL).map(bibField -> new BibField(bibField.getField(), bibField.getFieldPriority())).collect(Collectors.toList());

            BibEntryType newType = new BibEntryType(type.getType(), otherFields, requiredFields);
            entryTypesManager.addCustomOrModifiedType(newType, mode);
        }

        for (var type : typesToRemove) {
            entryTypesManager.removeCustomOrModifiedEntryType(type, mode);
        }
        preferencesService.saveCustomEntryTypes();
        //Reload types from preferences to make sure any modifications are present when reopening the dialog
        entryTypesManager.addCustomOrModifiedTypes(preferencesService.loadBibEntryTypes(BibDatabaseMode.BIBTEX),
                                                   preferencesService.loadBibEntryTypes(BibDatabaseMode.BIBLATEX));
    }

}
