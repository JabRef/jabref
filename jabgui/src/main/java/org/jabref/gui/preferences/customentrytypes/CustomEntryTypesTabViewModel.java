package org.jabref.gui.preferences.customentrytypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class CustomEntryTypesTabViewModel implements PreferenceTabViewModel {

    private final ObservableList<Field> fieldsForAdding = FXCollections.observableArrayList(FieldFactory.getStandardFieldsWithCitationKey());
    private final ObjectProperty<EntryTypeViewModel> selectedEntryType = new SimpleObjectProperty<>();
    private final StringProperty entryTypeToAdd = new SimpleStringProperty("");
    private final StringProperty newFieldToAdd = new SimpleStringProperty("");
    private final ObservableList<EntryTypeViewModel> entryTypesWithFields = FXCollections.observableArrayList(extractor -> new Observable[] {extractor.entryType(), extractor.fields()});
    private final List<BibEntryType> entryTypesToDelete = new ArrayList<>();
    /// State at dialog open, so that resets (which store immediately) also count as changes
    private Set<String> storedEntryTypes;
    private Set<Field> storedMultilineFields;
    private Optional<String> restartWarning = Optional.empty();

    private final CliPreferences preferences;
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
                                        CliPreferences preferences) {
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;
        this.dialogService = dialogService;
        this.bibDatabaseMode = mode;

        this.multiLineFields.addAll(preferences.getFieldPreferences().getNonWrappableFields());
        this.storedEntryTypes = entryTypeDefinitions();
        this.storedMultilineFields = multilineFieldsOfEntryTypes();

        entryTypeValidator = new FunctionBasedValidator<>(
                entryTypeToAdd,
                input -> StringUtil.isNotBlank(input) && !input.contains(" "),
                ValidationMessage.error(Localization.lang("Entry type cannot be empty and must not contain spaces.")));
        fieldValidator = new FunctionBasedValidator<>(
                newFieldToAdd,
                input -> StringUtil.isNotBlank(input) && !input.contains(" "),
                ValidationMessage.error(Localization.lang("Field cannot be empty and must not contain spaces."))
        );
    }

    @Override
    public void setValues() {
        if (!this.entryTypesWithFields.isEmpty()) {
            this.entryTypesWithFields.clear();
            this.resetMultilineFieldsToDefault();
        }
        Collection<BibEntryType> allTypes = entryTypesManager.getAllTypes(bibDatabaseMode);

        for (BibEntryType entryType : allTypes) {
            EntryTypeViewModel viewModel;
            if (entryTypesManager.isCustomType(entryType, bibDatabaseMode)) {
                viewModel = new CustomEntryTypeViewModel(entryType, isMultiline);
            } else {
                viewModel = new EntryTypeViewModel(entryType, isMultiline);
            }
            this.entryTypesWithFields.add(viewModel);
        }
    }

    @Override
    public void storeSettings() {
        // Collected across all entry types, applied to the non-wrappable fields preference after the loop
        Set<Field> singleLineFields = new HashSet<>();
        Set<Field> multilineFields = new HashSet<>();
        for (EntryTypeViewModel typeViewModel : entryTypesWithFields) {
            List<FieldViewModel> allFields = typeViewModel.fields();

            BibEntryType type = typeViewModel.entryType().getValue();
            EntryType newPlainType = type.getType();

            singleLineFields.addAll(allFields.stream()
                                             .filter(model -> !model.isMultiline())
                                             .map(model -> model.toField(newPlainType))
                                             .toList());
            multilineFields.addAll(allFields.stream()
                                            .filter(FieldViewModel::isMultiline)
                                            .map(model -> model.toField(newPlainType))
                                            .toList());

            List<OrFields> required = allFields.stream()
                                               .filter(FieldViewModel::isRequired)
                                               .map(model -> model.toField(newPlainType))
                                               .map(OrFields::new)
                                               .collect(Collectors.toList());

            List<BibField> fields = allFields.stream().map(model -> model.toBibField(newPlainType)).collect(Collectors.toList());

            BibEntryType newType = new BibEntryType(newPlainType, fields, required);

            entryTypesManager.update(newType, bibDatabaseMode);
        }

        for (BibEntryType entryType : entryTypesToDelete) {
            entryTypesManager.removeCustomOrModifiedEntryType(entryType, bibDatabaseMode);
        }

        // Fields of no entry type (e.g., [StandardField#PS]) keep their state: this tab does not show them
        Set<Field> nonWrappableFields = new HashSet<>(preferences.getFieldPreferences().getNonWrappableFields());
        nonWrappableFields.removeAll(singleLineFields);
        nonWrappableFields.addAll(multilineFields);
        preferences.getFieldPreferences().setNonWrappableFields(nonWrappableFields);
        preferences.storeCustomEntryTypesRepository(entryTypesManager);

        restartWarning = detectEntryTypesChanged();
    }

    /// Compares the definitions with the state at dialog open and re-bases them for the next save.
    ///
    /// The entry types are compared in their serialized form, because `BibEntryType.equals` compares fields by name
    /// only - a changed field property would go unnoticed.
    /// The multiline state is kept out of that form: it lives in the preferences, not in the entry type.
    private Optional<String> detectEntryTypesChanged() {
        Set<String> entryTypes = entryTypeDefinitions();
        Set<Field> multilineFields = multilineFieldsOfEntryTypes();
        boolean changed = !storedEntryTypes.equals(entryTypes) || !storedMultilineFields.equals(multilineFields);
        storedEntryTypes = entryTypes;
        storedMultilineFields = multilineFields;
        return changed ? Optional.of(Localization.lang("Entry types changed.")) : Optional.empty();
    }

    private Set<String> entryTypeDefinitions() {
        return entryTypesManager.getAllTypes(bibDatabaseMode).stream()
                                .map(MetaDataSerializer::serializeCustomEntryTypesV2)
                                .collect(Collectors.toSet());
    }

    /// Multiline fields not belonging to any entry type are left out: this tab does not show them.
    ///
    /// The non-wrappable fields preference itself cannot be compared: a field is also multiline when it carries
    /// [FieldProperty#MULTILINE_TEXT] (e.g., `abstract`), and saving adds those to the preference - the first
    /// unchanged save would thus look like a change.
    private Set<Field> multilineFieldsOfEntryTypes() {
        List<Field> nonWrappableFields = preferences.getFieldPreferences().getNonWrappableFields();
        return entryTypesManager.getAllTypes(bibDatabaseMode).stream()
                                .flatMap(type -> type.getAllFields().stream())
                                .filter(field -> nonWrappableFields.contains(field) || field.getProperties().contains(FieldProperty.MULTILINE_TEXT))
                                .collect(Collectors.toSet());
    }

    @Override
    public List<String> getRestartWarnings() {
        return restartWarning.map(List::of).orElseGet(List::of);
    }

    public EntryTypeViewModel addNewCustomEntryType() {
        EntryType newentryType = new UnknownEntryType(entryTypeToAdd.getValue());
        BibEntryType type = new BibEntryType(newentryType, new ArrayList<>(), List.of());
        EntryTypeViewModel entryTypeViewModel = new CustomEntryTypeViewModel(type, isMultiline);
        this.entryTypesWithFields.add(entryTypeViewModel);
        this.entryTypeToAdd.setValue("");

        return entryTypeViewModel;
    }

    public void removeEntryType(EntryTypeViewModel focusedItem) {
        entryTypesWithFields.remove(focusedItem);
        entryTypesToDelete.add(focusedItem.entryType().getValue());
    }

    public Optional<FieldViewModel> addNewField(List<FieldProperty> selectedProperties) {
        String fieldName = newFieldToAdd.get().trim();
        EntryType entryType = selectedEntryType.getValue().entryType().getValue().getType();
        Field newField = FieldFactory.parseField(entryType, fieldName);
        Optional<FieldViewModel> existingFieldViewModel = findExistingField(newField);
        if (existingFieldViewModel.isPresent()) {
            FieldViewModel field = existingFieldViewModel.get();
            if (newField.isStandardField()) {
                dialogService.showWarningDialogAndWait(
                        Localization.lang("Duplicate fields"),
                        Localization.lang("Warning: You added field \"%0\" twice. Only one will be kept.", FieldTextMapper.getDisplayName(newField)));

                return existingFieldViewModel;
            }

            // customized field
            ObservableList<FieldProperty> fieldProperties = field.getProperties();
            if ((!fieldProperties.isEmpty() && fieldProperties.equals(selectedProperties))
                    || (fieldProperties.isEmpty() && fieldProperties.equals(selectedProperties))) {
                dialogService.showWarningDialogAndWait(
                        Localization.lang("Duplicate properties"),
                        Localization.lang("Warning: Current properties are the same as before."));
                return existingFieldViewModel;
            }

            field.getProperties().clear();
            field.getProperties().addAll(selectedProperties);

            // TODO： update action is not visually obvious, can use notify function in JabRefDialogService.java
            return existingFieldViewModel;
        }

        FieldViewModel fieldViewModel = new FieldViewModel(newField,
                FieldViewModel.Mandatory.REQUIRED,
                FieldPriority.IMPORTANT,
                isMultiline.test(newField));
        fieldViewModel.getProperties().addAll(selectedProperties);

        this.selectedEntryType.getValue().addField(fieldViewModel);
        newFieldToAdd.set("");

        return Optional.of(fieldViewModel);
    }

    private Optional<FieldViewModel> findExistingField(Field field) {
        String displayName = FieldTextMapper.getDisplayName(field);
        return selectedEntryType.getValue()
                                .fields()
                                .stream()
                                .filter(fieldViewModel ->
                                        fieldViewModel.displayNameProperty().getValue()
                                                      .equalsIgnoreCase(displayName))
                                .findFirst();
    }

    public boolean displayNameExists(String displayName) {
        ObservableList<FieldViewModel> entryFields = this.selectedEntryType.getValue().fields();
        return entryFields.stream().anyMatch(fieldViewModel ->
                fieldViewModel.displayNameProperty().getValue().equalsIgnoreCase(displayName));
    }

    public void removeField(FieldViewModel focusedItem) {
        selectedEntryType.getValue().removeField(focusedItem);
    }

    public void resetAllCustomEntryTypes() {
        entryTypesManager.clearAllCustomEntryTypes(bibDatabaseMode);
        preferences.storeCustomEntryTypesRepository(entryTypesManager);
    }

    public ObjectProperty<EntryTypeViewModel> selectedEntryTypeProperty() {
        return this.selectedEntryType;
    }

    public StringProperty entryTypeToAddProperty() {
        return this.entryTypeToAdd;
    }

    public StringProperty newFieldToAddProperty() {
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

    public void resetMultilineFieldsToDefault() {
        resetStandardFieldMultilineToDefaults();
        List<Field> defaultNonWrappableFields = FieldPreferences.getDefault().getNonWrappableFields();
        preferences.getFieldPreferences().setNonWrappableFields(defaultNonWrappableFields);
        multiLineFields.clear();
        multiLineFields.addAll(defaultNonWrappableFields);
    }

    private void resetStandardFieldMultilineToDefaults() {
        for (StandardField field : StandardField.values()) {
            if (StandardField.BUILT_IN_MULTILINE_FIELDS.contains(field)) {
                field.getProperties().add(FieldProperty.MULTILINE_TEXT);
            } else {
                field.getProperties().remove(FieldProperty.MULTILINE_TEXT);
            }
        }
    }
}
