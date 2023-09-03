package org.jabref.gui.preferences.customentrytypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.UnknownEntryType;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.PreferencesService;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class CustomEntryTypesTabViewModel implements PreferenceTabViewModel {

    private final ObservableList<Field> fieldsForAdding = FXCollections.observableArrayList(FieldFactory.getStandardFieldsWithCitationKey());
    private final ObjectProperty<EntryTypeViewModel> selectedEntryType = new SimpleObjectProperty<>();
    private final StringProperty entryTypeToAdd = new SimpleStringProperty("");
    private final ObjectProperty<Field> newFieldToAdd = new SimpleObjectProperty<>();
    private final ObservableList<EntryTypeViewModel> entryTypesWithFields = FXCollections.observableArrayList(extractor -> new Observable[]{extractor.entryType(), extractor.fields()});
    private final List<BibEntryType> entryTypesToDelete = new ArrayList<>();

    private final PreferencesService preferencesService;
    private final BibEntryTypesManager entryTypesManager;
    private final DialogService dialogService;
    private final BibDatabaseMode bibDatabaseMode;

    private final Validator entryTypeValidator;
    private final Validator fieldValidator;
    private final Set<Field> multiLineFields = new HashSet<>();

    Predicate<Field> isMultiline = field -> this.multiLineFields.contains(field) || field.getProperties().contains(FieldProperty.MULTILINE_TEXT);

    public CustomEntryTypesTabViewModel(BibDatabaseMode mode,
                                        BibEntryTypesManager entryTypesManager,
                                        DialogService dialogService,
                                        PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
        this.entryTypesManager = entryTypesManager;
        this.dialogService = dialogService;
        this.bibDatabaseMode = mode;

        this.multiLineFields.addAll(preferencesService.getFieldPreferences().getNonWrappableFields());

        entryTypeValidator = new FunctionBasedValidator<>(
                entryTypeToAdd,
                StringUtil::isNotBlank,
                ValidationMessage.error(Localization.lang("Entry type cannot be empty. Please enter a name.")));
        fieldValidator = new FunctionBasedValidator<>(
                newFieldToAdd,
                input -> (input != null) && StringUtil.isNotBlank(input.getDisplayName()),
                ValidationMessage.error(Localization.lang("Field cannot be empty. Please enter a name.")));
    }

    @Override
    public void setValues() {
        if (!this.entryTypesWithFields.isEmpty()) {
            this.entryTypesWithFields.clear();
        }
        Collection<BibEntryType> allTypes = entryTypesManager.getAllTypes(bibDatabaseMode);

        for (BibEntryType entryType : allTypes) {
            EntryTypeViewModel viewModel;
            if (entryTypesManager.isCustomType(entryType.getType(), bibDatabaseMode)) {
                viewModel = new CustomEntryTypeViewModel(entryType, isMultiline);
            } else {
                viewModel = new EntryTypeViewModel(entryType, isMultiline);
            }
            this.entryTypesWithFields.add(viewModel);
        }
    }

    @Override
    public void storeSettings() {
        Set<Field> multilineFields = new HashSet<>();
        for (EntryTypeViewModel typeViewModel : entryTypesWithFields) {
            BibEntryType type = typeViewModel.entryType().getValue();
            List<FieldViewModel> allFields = typeViewModel.fields();

            multilineFields.addAll(allFields.stream()
                                            .filter(FieldViewModel::isMultiline)
                                            .map(FieldViewModel::toField)
                                            .toList());

            List<OrFields> required = allFields.stream()
                                               .filter(FieldViewModel::isRequired)
                                               .map(FieldViewModel::toField)
                                               .map(OrFields::new)
                                               .collect(Collectors.toList());
            List<BibField> fields = allFields.stream().map(FieldViewModel::toBibField).collect(Collectors.toList());

            BibEntryType newType = new BibEntryType(type.getType(), fields, required);
            entryTypesManager.addCustomOrModifiedType(newType, bibDatabaseMode);
        }

        for (var entryType : entryTypesToDelete) {
            entryTypesManager.removeCustomOrModifiedEntryType(entryType, bibDatabaseMode);
        }

        preferencesService.getFieldPreferences().setNonWrappableFields(multilineFields);
        preferencesService.storeCustomEntryTypesRepository(entryTypesManager);
    }

    public EntryTypeViewModel addNewCustomEntryType() {
        EntryType newentryType = new UnknownEntryType(entryTypeToAdd.getValue());
        BibEntryType type = new BibEntryType(newentryType, new ArrayList<>(), Collections.emptyList());
        EntryTypeViewModel viewModel = new CustomEntryTypeViewModel(type, isMultiline);
        this.entryTypesWithFields.add(viewModel);
        this.entryTypeToAdd.setValue("");

        return viewModel;
    }

    public void removeEntryType(EntryTypeViewModel focusedItem) {
        entryTypesWithFields.remove(focusedItem);
        entryTypesToDelete.add(focusedItem.entryType().getValue());
    }

    public void addNewField() {
        Field field = newFieldToAdd.getValue();
        boolean fieldExists = displayNameExists(field.getDisplayName());

        if (fieldExists) {
            dialogService.showWarningDialogAndWait(
                    Localization.lang("Duplicate fields"),
                    Localization.lang("Warning: You added field \"%0\" twice. Only one will be kept.", field.getDisplayName()));
        } else {
            this.selectedEntryType.getValue().addField(new FieldViewModel(
                    field,
                    FieldViewModel.Mandatory.REQUIRED,
                    FieldPriority.IMPORTANT,
                    false));
        }
        newFieldToAddProperty().setValue(null);
    }

    public boolean displayNameExists(String displayName) {
        ObservableList<FieldViewModel> entryFields = this.selectedEntryType.getValue().fields();
        return entryFields.stream().anyMatch(fieldViewModel ->
                fieldViewModel.displayNameProperty().getValue().equals(displayName));
    }

    public void removeField(FieldViewModel focusedItem) {
        selectedEntryType.getValue().removeField(focusedItem);
    }

    public void resetAllCustomEntryTypes() {
        entryTypesManager.clearAllCustomEntryTypes(bibDatabaseMode);
        preferencesService.storeCustomEntryTypesRepository(entryTypesManager);
    }

    public ObjectProperty<EntryTypeViewModel> selectedEntryTypeProperty() {
        return this.selectedEntryType;
    }

    public StringProperty entryTypeToAddProperty() {
        return this.entryTypeToAdd;
    }

    public ObjectProperty<Field> newFieldToAddProperty() {
        return this.newFieldToAdd;
    }

    public ObservableList<EntryTypeViewModel> entryTypes() {
        return this.entryTypesWithFields;
    }

    public ObservableList<Field> fieldsForAdding() {
        return this.fieldsForAdding;
    }

    public ValidationStatus entryTypeValidationStatus() {
        return entryTypeValidator.getValidationStatus();
    }

    public ValidationStatus fieldValidationStatus() {
        return fieldValidator.getValidationStatus();
    }
}
