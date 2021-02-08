package org.jabref.gui.preferences.file;

import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.preferences.ImportExportPreferences;
import org.jabref.preferences.NewLineSeparator;
import org.jabref.preferences.PreferencesService;

public class FileTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty openLastStartupProperty = new SimpleBooleanProperty();
    private final StringProperty noWrapFilesProperty = new SimpleStringProperty("");
    private final BooleanProperty resolveStringsBibTexProperty = new SimpleBooleanProperty();
    private final BooleanProperty resolveStringsAllProperty = new SimpleBooleanProperty();
    private final StringProperty resolveStringsExceptProperty = new SimpleStringProperty("");
    private final ListProperty<NewLineSeparator> newLineSeparatorListProperty = new SimpleListProperty<>();
    private final ObjectProperty<NewLineSeparator> selectedNewLineSeparatorProperty = new SimpleObjectProperty<>();
    private final BooleanProperty alwaysReformatBibProperty = new SimpleBooleanProperty();

    // SaveOrderConfigPanel
    private final BooleanProperty saveInOriginalProperty = new SimpleBooleanProperty();
    private final BooleanProperty saveInTableOrderProperty = new SimpleBooleanProperty();
    private final BooleanProperty saveInSpecifiedOrderProperty = new SimpleBooleanProperty();
    // ToDo: The single criterions should really be a map or a list.
    private final ListProperty<Field> primarySortFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Field> secondarySortFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Field> tertiarySortFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final BooleanProperty savePrimaryDescPropertySelected = new SimpleBooleanProperty();
    private final BooleanProperty saveSecondaryDescPropertySelected = new SimpleBooleanProperty();
    private final BooleanProperty saveTertiaryDescPropertySelected = new SimpleBooleanProperty();
    private final ObjectProperty<Field> savePrimarySortSelectedValueProperty = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Field> saveSecondarySortSelectedValueProperty = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Field> saveTertiarySortSelectedValueProperty = new SimpleObjectProperty<>(null);

    private final BooleanProperty autosaveLocalLibraries = new SimpleBooleanProperty();

    private final PreferencesService preferences;
    private final SaveOrderConfig initialExportOrder;
    private final ImportExportPreferences initialImportExportPreferences;

    FileTabViewModel(PreferencesService preferences) {
        this.preferences = preferences;
        this.initialExportOrder = preferences.loadExportSaveOrder();
        this.initialImportExportPreferences = preferences.getImportExportPreferences();
    }

    @Override
    public void setValues() {
        openLastStartupProperty.setValue(preferences.shouldOpenLastFilesOnStartup());

        noWrapFilesProperty.setValue(initialImportExportPreferences.getNonWrappableFields());
        resolveStringsAllProperty.setValue(initialImportExportPreferences.shouldResolveStringsForAllStrings()); // Flipped around
        resolveStringsBibTexProperty.setValue(initialImportExportPreferences.shouldResolveStringsForStandardBibtexFields());
        resolveStringsExceptProperty.setValue(initialImportExportPreferences.getNonResolvableFields());
        newLineSeparatorListProperty.setValue(FXCollections.observableArrayList(NewLineSeparator.values()));
        selectedNewLineSeparatorProperty.setValue(initialImportExportPreferences.getNewLineSeparator());

        alwaysReformatBibProperty.setValue(initialImportExportPreferences.shouldAlwaysReformatOnSave());

        if (initialExportOrder.saveInOriginalOrder()) {
            saveInOriginalProperty.setValue(true);
        } else if (initialExportOrder.saveInSpecifiedOrder()) {
            saveInSpecifiedOrderProperty.setValue(true);
        } else {
            saveInTableOrderProperty.setValue(true);
        }

        Set<Field> fieldNames = FieldFactory.getCommonFields();
        primarySortFieldsProperty.addAll(fieldNames);
        secondarySortFieldsProperty.addAll(fieldNames);
        tertiarySortFieldsProperty.addAll(fieldNames);

        savePrimarySortSelectedValueProperty.setValue(initialExportOrder.getSortCriteria().get(0).field);
        saveSecondarySortSelectedValueProperty.setValue(initialExportOrder.getSortCriteria().get(1).field);
        saveTertiarySortSelectedValueProperty.setValue(initialExportOrder.getSortCriteria().get(2).field);

        savePrimaryDescPropertySelected.setValue(initialExportOrder.getSortCriteria().get(0).descending);
        saveSecondaryDescPropertySelected.setValue(initialExportOrder.getSortCriteria().get(1).descending);
        saveTertiaryDescPropertySelected.setValue(initialExportOrder.getSortCriteria().get(2).descending);

        autosaveLocalLibraries.setValue(preferences.shouldAutosave());
    }

    @Override
    public void storeSettings() {
        preferences.storeOpenLastFilesOnStartup(openLastStartupProperty.getValue());

        ImportExportPreferences newImportExportPreferences = new ImportExportPreferences(
                noWrapFilesProperty.getValue().trim(),
                resolveStringsBibTexProperty.getValue(),
                resolveStringsAllProperty.getValue(),
                resolveStringsExceptProperty.getValue().trim(),
                selectedNewLineSeparatorProperty.getValue(),
                alwaysReformatBibProperty.getValue(),
                initialImportExportPreferences.getImportWorkingDirectory(),
                initialImportExportPreferences.getLastExportExtension(),
                initialImportExportPreferences.getExportWorkingDirectory());
        preferences.storeImportExportPreferences(newImportExportPreferences);

        SaveOrderConfig newSaveOrderConfig = new SaveOrderConfig(
                saveInOriginalProperty.getValue(),
                saveInSpecifiedOrderProperty.getValue(),
                new SaveOrderConfig.SortCriterion(
                        savePrimarySortSelectedValueProperty.get(),
                        savePrimaryDescPropertySelected.getValue()),
                new SaveOrderConfig.SortCriterion(
                        saveSecondarySortSelectedValueProperty.get(),
                        saveSecondaryDescPropertySelected.getValue()),
                new SaveOrderConfig.SortCriterion(
                        saveTertiarySortSelectedValueProperty.get(),
                        saveTertiaryDescPropertySelected.getValue()));
        preferences.storeExportSaveOrder(newSaveOrderConfig);

        preferences.storeShouldAutosave(autosaveLocalLibraries.getValue());
    }

    // General

    public BooleanProperty openLastStartupProperty() {
        return openLastStartupProperty;
    }

    public StringProperty noWrapFilesProperty() {
        return noWrapFilesProperty;
    }

    public BooleanProperty resolveStringsBibTexProperty() {
        return resolveStringsBibTexProperty;
    }

    public BooleanProperty resolveStringsAllProperty() {
        return resolveStringsAllProperty;
    }

    public StringProperty resolvStringsExceptProperty() {
        return resolveStringsExceptProperty;
    }

    public ListProperty<NewLineSeparator> newLineSeparatorListProperty() {
        return newLineSeparatorListProperty;
    }

    public ObjectProperty<NewLineSeparator> selectedNewLineSeparatorProperty() {
        return selectedNewLineSeparatorProperty;
    }

    public BooleanProperty alwaysReformatBibProperty() {
        return alwaysReformatBibProperty;
    }

    // Autosave
    public BooleanProperty autosaveLocalLibrariesProperty() {
        return autosaveLocalLibraries;
    }

    // SaveOrderConfigPanel

    public BooleanProperty saveInOriginalProperty() {
        return saveInOriginalProperty;
    }

    public BooleanProperty saveInTableOrderProperty() {
        return saveInTableOrderProperty;
    }

    public BooleanProperty saveInSpecifiedOrderProperty() {
        return saveInSpecifiedOrderProperty;
    }

    public ListProperty<Field> primarySortFieldsProperty() {
        return primarySortFieldsProperty;
    }

    public ListProperty<Field> secondarySortFieldsProperty() {
        return secondarySortFieldsProperty;
    }

    public ListProperty<Field> tertiarySortFieldsProperty() {
        return tertiarySortFieldsProperty;
    }

    public ObjectProperty<Field> savePrimarySortSelectedValueProperty() {
        return savePrimarySortSelectedValueProperty;
    }

    public ObjectProperty<Field> saveSecondarySortSelectedValueProperty() {
        return saveSecondarySortSelectedValueProperty;
    }

    public ObjectProperty<Field> saveTertiarySortSelectedValueProperty() {
        return saveTertiarySortSelectedValueProperty;
    }

    public BooleanProperty savePrimaryDescPropertySelected() {
        return savePrimaryDescPropertySelected;
    }

    public BooleanProperty saveSecondaryDescPropertySelected() {
        return saveSecondaryDescPropertySelected;
    }

    public BooleanProperty saveTertiaryDescPropertySelected() {
        return saveTertiaryDescPropertySelected;
    }
}
