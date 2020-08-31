package org.jabref.gui.preferences;

import java.util.List;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.preferences.PreferencesService;

public class ExportSortingTabViewModel implements PreferenceTabViewModel {
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

    private final PreferencesService preferences;

    ExportSortingTabViewModel(PreferencesService preferences) {
        this.preferences = preferences;
    }

    @Override
    public void setValues() {
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
    }

    @Override
    public void storeSettings() {
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
    }

    @Override
    public boolean validateSettings() {
        return false;
    }

    @Override
    public List<String> getRestartWarnings() {
        return null;
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
