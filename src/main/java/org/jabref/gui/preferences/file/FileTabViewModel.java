package org.jabref.gui.preferences.file;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.commonfxcontrols.SortCriterionViewModel;
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
    private final ListProperty<Field> sortableFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<SortCriterionViewModel> sortCriteriaProperty = new SimpleListProperty<>(FXCollections.observableArrayList(new LinkedList<>()));

    private final BooleanProperty autosaveLocalLibraries = new SimpleBooleanProperty();

    private final PreferencesService preferences;
    private final SaveOrderConfig initialExportOrder;
    private final ImportExportPreferences initialImportExportPreferences;

    FileTabViewModel(PreferencesService preferences) {
        this.preferences = preferences;
        this.initialExportOrder = preferences.getExportSaveOrder();
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

        switch (initialExportOrder.getOrderType()) {
            case SPECIFIED -> saveInSpecifiedOrderProperty.setValue(true);
            case ORIGINAL -> saveInOriginalProperty.setValue(true);
            case TABLE -> saveInTableOrderProperty.setValue(true);
        }

        List<Field> fieldNames = new ArrayList<>(FieldFactory.getCommonFields());
        fieldNames.sort(Comparator.comparing(Field::getDisplayName));

        sortableFieldsProperty.addAll(fieldNames);
        sortCriteriaProperty.addAll(initialExportOrder.getSortCriteria().stream()
                                                      .map(SortCriterionViewModel::new)
                                                      .collect(Collectors.toList()));

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
                SaveOrderConfig.OrderType.fromBooleans(saveInSpecifiedOrderProperty.getValue(), saveInTableOrderProperty.getValue()),
                new LinkedList<>(sortCriteriaProperty.stream().map(SortCriterionViewModel::getCriterion).toList()));
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

    public StringProperty resolveStringsExceptProperty() {
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

    public ListProperty<Field> sortableFieldsProperty() {
        return sortableFieldsProperty;
    }

    public ListProperty<SortCriterionViewModel> sortCriteriaProperty() {
        return sortCriteriaProperty;
    }
}
