package org.jabref.gui.customentrytypes;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;
import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.*;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.UnknownEntryType;
import org.jabref.preferences.PreferencesService;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CustomEntryTypeDialogViewModel {

    public static final StringConverter<Field> FIELD_STRING_CONVERTER = new StringConverter<>() {
        @Override
        public String toString(Field object) {
            return object != null ? object.getDisplayName() : "";
        }

        @Override
        public Field fromString(String string) {
            return FieldFactory.parseField(string);
        }
    };

    private final ObservableList<Field> fieldsForAdding = FXCollections.observableArrayList(FieldFactory.getStandardFieldsWithCitationKey());
    private final ObjectProperty<EntryTypeViewModel> selectedEntryType = new SimpleObjectProperty<>();
    private final ObjectProperty<Field> selectedFieldToAdd = new SimpleObjectProperty<>();
    private final StringProperty entryTypeToAdd = new SimpleStringProperty("");
    private final ObjectProperty<Field> newFieldToAdd = new SimpleObjectProperty<>();
    private final BibDatabaseMode mode;
    private final ObservableList<EntryTypeViewModel> entryTypesWithFields = FXCollections.observableArrayList(extractor -> new Observable[] {extractor.entryType(), extractor.fields()});
    private final List<BibEntryType> entryTypesToDelete = new ArrayList<>();

    private final PreferencesService preferencesService;
    private final BibEntryTypesManager entryTypesManager;
    private final DialogService dialogService;

    private final Validator entryTypeValidator;
    private final Validator fieldValidator;

    public CustomEntryTypeDialogViewModel(BibDatabaseMode mode, PreferencesService preferencesService, BibEntryTypesManager entryTypesManager, DialogService dialogService) {
        this.mode = mode;
        this.preferencesService = preferencesService;
        this.entryTypesManager = entryTypesManager;
        this.dialogService = dialogService;

        addAllTypes();

        Predicate<String> notEmpty = input -> (input != null) && !input.trim().isEmpty();
        entryTypeValidator = new FunctionBasedValidator<>(entryTypeToAdd, notEmpty, ValidationMessage.error(Localization.lang("Entry type cannot be empty. Please enter a name.")));
        fieldValidator = new FunctionBasedValidator<>(newFieldToAdd,
                                                      input -> (input != null) && !input.getDisplayName().isEmpty(),
                                                      ValidationMessage.error(Localization.lang("Field cannot be empty. Please enter a name.")));
    }

    public void addAllTypes() {
        if (this.entryTypesWithFields.size() > 0) {
            this.entryTypesWithFields.clear();
        }
        Collection<BibEntryType> allTypes = entryTypesManager.getAllTypes(mode);

        for (BibEntryType entryType : allTypes) {
            EntryTypeViewModel viewModel;
            if (entryTypesManager.isCustomType(entryType.getType(), mode)) {
                viewModel = new CustomEntryTypeViewModel(entryType);
            } else {
                viewModel = new EntryTypeViewModel(entryType);
            }
            this.entryTypesWithFields.add(viewModel);
        }
    }

    public ObservableList<EntryTypeViewModel> entryTypes() {
        return this.entryTypesWithFields;
    }

    public ObservableList<Field> fieldsForAdding() {
        return this.fieldsForAdding;
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

        boolean fieldExists = false;

        // create list with all the entry's fields
        ObservableList<FieldViewModel> entryFields = this.selectedEntryType.getValue().fields();

        // compare every entry field name with the user field name in order to find out if any of them has the same one. If so, show warning.
        for (FieldViewModel fieldViewModel : entryFields) {
            if (Objects.equals(fieldViewModel.fieldName().getValue(), field.getDisplayName())) {
                dialogService.showWarningDialogAndWait(Localization.lang("Duplicate fields"),
                        Localization.lang("Warning: You added field \"%0\" twice. Only one will be kept.", field.getDisplayName()));
                fieldExists = true;
                break;
            }
        }

        // if the user field name isn't found inside the list, pass it to the entry as a new one.
        if (!fieldExists) this.selectedEntryType.getValue().addField(model);


        newFieldToAddProperty().setValue(null);
    }

    public EntryTypeViewModel addNewCustomEntryType() {
        EntryType newentryType = new UnknownEntryType(entryTypeToAdd.getValue());
        BibEntryType type = new BibEntryType(newentryType, new ArrayList<>(), Collections.emptyList());
        EntryTypeViewModel viewModel = new CustomEntryTypeViewModel(type);
        this.entryTypesWithFields.add(viewModel);
        this.entryTypeToAdd.setValue("");

        return viewModel;
    }

    public ObjectProperty<EntryTypeViewModel> selectedEntryTypeProperty() {
        return this.selectedEntryType;
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

    public void removeEntryType(EntryTypeViewModel focusedItem) {
        entryTypesWithFields.remove(focusedItem);
        entryTypesToDelete.add(focusedItem.entryType().getValue());
    }

    public void removeField(FieldViewModel focusedItem) {
       selectedEntryType.getValue().removeField(focusedItem);
    }

    public void resetAllCustomEntryTypes() {
        entryTypesManager.clearAllCustomEntryTypes(mode);
        preferencesService.clearBibEntryTypes(mode);
        entryTypesManager.addCustomOrModifiedTypes(preferencesService.getBibEntryTypes(BibDatabaseMode.BIBTEX),
                                                   preferencesService.getBibEntryTypes(BibDatabaseMode.BIBLATEX));
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

        for (var entryType : entryTypesToDelete) {
            entryTypesManager.removeCustomOrModifiedEntryType(entryType, mode);
        }

        preferencesService.storeCustomEntryTypes(entryTypesManager);
        // Reload types from preferences to make sure any modifications are present when reopening the dialog
        entryTypesManager.addCustomOrModifiedTypes(preferencesService.getBibEntryTypes(BibDatabaseMode.BIBTEX),
                                                   preferencesService.getBibEntryTypes(BibDatabaseMode.BIBLATEX));
    }
}
