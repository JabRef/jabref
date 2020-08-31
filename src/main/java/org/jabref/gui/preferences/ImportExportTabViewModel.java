package org.jabref.gui.preferences;

import java.util.List;
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

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.NewLineSeparator;

public class ImportExportTabViewModel implements PreferenceTabViewModel {

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

    private final JabRefPreferences preferences;

    ImportExportTabViewModel(JabRefPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public void setValues() {
        openLastStartupProperty.setValue(preferences.getBoolean(JabRefPreferences.OPEN_LAST_EDITED));
        noWrapFilesProperty.setValue(preferences.get(JabRefPreferences.NON_WRAPPABLE_FIELDS));
        resolveStringsAllProperty.setValue(preferences.getBoolean(JabRefPreferences.RESOLVE_STRINGS_ALL_FIELDS)); // Flipped around
        resolveStringsBibTexProperty.setValue(!resolveStringsAllProperty.getValue());
        resolveStringsExceptProperty.setValue(preferences.get(JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR));
        newLineSeparatorListProperty.setValue(FXCollections.observableArrayList(NewLineSeparator.values()));
        selectedNewLineSeparatorProperty.setValue(preferences.getNewLineSeparator());

        alwaysReformatBibProperty.setValue(preferences.getBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT));

        SaveOrderConfig initialExportOrder = preferences.loadExportSaveOrder();

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

        autosaveLocalLibraries.setValue(preferences.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE));
    }

    @Override
    public void storeSettings() {

        preferences.putBoolean(JabRefPreferences.OPEN_LAST_EDITED, openLastStartupProperty.getValue());
        if (!noWrapFilesProperty.getValue().trim().equals(preferences.get(JabRefPreferences.NON_WRAPPABLE_FIELDS))) {
            preferences.put(JabRefPreferences.NON_WRAPPABLE_FIELDS, noWrapFilesProperty.getValue());
        }
        preferences.putBoolean(JabRefPreferences.RESOLVE_STRINGS_ALL_FIELDS, resolveStringsAllProperty.getValue());
        preferences.put(JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR, resolveStringsExceptProperty.getValue().trim());
        resolveStringsExceptProperty.setValue(preferences.get(JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR));
        preferences.storeNewLineSeparator(selectedNewLineSeparatorProperty.getValue());
        preferences.putBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT, alwaysReformatBibProperty.getValue());

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

        preferences.putBoolean(JabRefPreferences.LOCAL_AUTO_SAVE, autosaveLocalLibraries.getValue());
    }

    @Override
    public boolean validateSettings() {
        return false;
    }

    @Override
    public List<String> getRestartWarnings() {
        return null;
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
