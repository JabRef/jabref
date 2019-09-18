package org.jabref.gui;

import java.util.Objects;
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
import org.jabref.model.metadata.SaveOrderConfig.SortCriterion;
import org.jabref.preferences.PreferencesService;

public class SaveOrderConfigDisplayViewModel {

    private final ListProperty<Field> priSortFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Field> secSortFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Field> terSortFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final BooleanProperty savePriDescPropertySelected = new SimpleBooleanProperty();
    private final BooleanProperty saveSecDescPropertySelected = new SimpleBooleanProperty();
    private final BooleanProperty saveTerDescPropertySelected = new SimpleBooleanProperty();

    private final ObjectProperty<Field> savePriSortSelectedValueProperty = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Field> saveSecSortSelectedValueProperty = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Field> saveTerSortSelectedValueProperty = new SimpleObjectProperty<>(null);

    private final BooleanProperty saveInOriginalProperty = new SimpleBooleanProperty();
    private final BooleanProperty saveInTableOrderProperty = new SimpleBooleanProperty();
    private final BooleanProperty saveInSpecifiedOrderProperty = new SimpleBooleanProperty();

    private final PreferencesService prefs;

    public SaveOrderConfigDisplayViewModel(PreferencesService prefs) {
        this.prefs = prefs;

        Set<Field> fieldNames = FieldFactory.getCommonFields();
        priSortFieldsProperty.addAll(fieldNames);
        secSortFieldsProperty.addAll(fieldNames);
        terSortFieldsProperty.addAll(fieldNames);
    }

    public ListProperty<Field> priSortFieldsProperty() {
        return priSortFieldsProperty;
    }

    public ListProperty<Field> secSortFieldsProperty() {
        return secSortFieldsProperty;
    }

    public ListProperty<Field> terSortFieldsProperty() {
        return terSortFieldsProperty;
    }

    public SaveOrderConfig getSaveOrderConfig() {
        SortCriterion primary = new SortCriterion(savePriSortSelectedValueProperty.get(), savePriDescPropertySelected.getValue());
        SortCriterion secondary = new SortCriterion(saveSecSortSelectedValueProperty.get(), saveSecDescPropertySelected.getValue());
        SortCriterion tertiary = new SortCriterion(saveTerSortSelectedValueProperty.get(), saveTerDescPropertySelected.getValue());

        return new SaveOrderConfig(saveInOriginalProperty.getValue(), saveInSpecifiedOrderProperty.getValue(), primary, secondary, tertiary);
    }

    public void setSaveOrderConfig(SaveOrderConfig saveOrderConfig) {
        Objects.requireNonNull(saveOrderConfig);

        savePriSortSelectedValueProperty.setValue(saveOrderConfig.getSortCriteria().get(0).field);
        savePriDescPropertySelected.setValue(saveOrderConfig.getSortCriteria().get(0).descending);
        saveSecSortSelectedValueProperty.setValue(saveOrderConfig.getSortCriteria().get(1).field);
        saveSecDescPropertySelected.setValue(saveOrderConfig.getSortCriteria().get(1).descending);
        saveTerSortSelectedValueProperty.setValue(saveOrderConfig.getSortCriteria().get(2).field);
        saveTerDescPropertySelected.setValue(saveOrderConfig.getSortCriteria().get(2).descending);

        if (saveOrderConfig.saveInOriginalOrder()) {
            saveInOriginalProperty.setValue(true);
        } else if (saveOrderConfig.saveInSpecifiedOrder()) {
            saveInSpecifiedOrderProperty.setValue(true);
        } else {
            saveInTableOrderProperty.setValue(true);
        }

    }

    public BooleanProperty savePriDescPropertySelected() {
        return savePriDescPropertySelected;
    }

    public BooleanProperty saveSecDescPropertySelected() {
        return saveSecDescPropertySelected;
    }

    public BooleanProperty saveTerDescPropertySelected() {
        return saveTerDescPropertySelected;
    }

    public ObjectProperty<Field> savePriSortSelectedValueProperty() {
        return savePriSortSelectedValueProperty;
    }

    public ObjectProperty<Field> saveSecSortSelectedValueProperty() {
        return saveSecSortSelectedValueProperty;
    }

    public ObjectProperty<Field> saveTerSortSelectedValueProperty() {
        return saveTerSortSelectedValueProperty;
    }

    public void storeConfigInPrefs() {
        prefs.storeExportSaveOrder(this.getSaveOrderConfig());
    }

    public BooleanProperty saveInOriginalProperty() {
        return saveInOriginalProperty;
    }

    public BooleanProperty saveInTableOrderProperty() {
        return saveInTableOrderProperty;
    }

    public BooleanProperty saveInSpecifiedOrderProperty() {
        return this.saveInSpecifiedOrderProperty;
    }
}
