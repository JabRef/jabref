package org.jabref.gui;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.model.metadata.SaveOrderConfig.SortCriterion;

public class SaveOrderConfigDisplayViewModel {

    private final ListProperty<String> priSortFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<String> secSortFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<String> terSortFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final BooleanProperty savePriDescPropertySelected = new SimpleBooleanProperty();
    private final BooleanProperty saveSecDescPropertySelected = new SimpleBooleanProperty();
    private final BooleanProperty saveTerDescPropertySelected = new SimpleBooleanProperty();

    private final StringProperty savePriSortSelectedValueProperty = new SimpleStringProperty("");
    private final StringProperty saveSecSortSelectedValueProperty = new SimpleStringProperty("");
    private final StringProperty saveTerSortSelectedValueProperty = new SimpleStringProperty("");

    private final BooleanProperty saveInOriginalProperty = new SimpleBooleanProperty();
    private final BooleanProperty saveInTableOrderProperty = new SimpleBooleanProperty();
    private final BooleanProperty saveInSpecifiedOrderProperty = new SimpleBooleanProperty();

    private final MetaData metadata;

    public SaveOrderConfigDisplayViewModel(MetaData metaData) {
        this.metadata = metaData;

        Optional<SaveOrderConfig> storedSaveOrderConfig = metaData.getSaveOrderConfig();

        List<String> fieldNames = InternalBibtexFields.getAllPublicFieldNames();
        fieldNames.add(BibEntry.KEY_FIELD);
        Collections.sort(fieldNames);

        priSortFieldsProperty.addAll(fieldNames);
        secSortFieldsProperty.addAll(fieldNames);
        terSortFieldsProperty.addAll(fieldNames);
    }

    public ListProperty<String> priSortFieldsProperty() {
        return priSortFieldsProperty;
    }

    public ListProperty<String> secSortFieldsProperty() {
        return secSortFieldsProperty;
    }

    public ListProperty<String> terSortFieldsProperty() {
        return terSortFieldsProperty;
    }

    public SaveOrderConfig getSaveOrderConfig() {
        SaveOrderConfig saveOrderConfig = new SaveOrderConfig();
        SortCriterion primary = new SortCriterion(getSelectedItemAsLowerCaseTrim(savePriSortSelectedValueProperty), savePriDescPropertySelected.getValue());
        saveOrderConfig.getSortCriteria().add(primary);
        SortCriterion secondary = new SortCriterion(getSelectedItemAsLowerCaseTrim(saveSecSortSelectedValueProperty), saveSecDescPropertySelected.getValue());
        saveOrderConfig.getSortCriteria().add(secondary);
        SortCriterion tertiary = new SortCriterion(getSelectedItemAsLowerCaseTrim(saveTerSortSelectedValueProperty), saveTerDescPropertySelected.getValue());
        saveOrderConfig.getSortCriteria().add(tertiary);

        return saveOrderConfig;
    }

    public void setSaveOrderConfig(SaveOrderConfig saveOrderConfig) {
        Objects.requireNonNull(saveOrderConfig);

        savePriSortSelectedValueProperty.setValue(saveOrderConfig.getSortCriteria().get(0).field);
        savePriDescPropertySelected.setValue(saveOrderConfig.getSortCriteria().get(0).descending);
        saveSecSortSelectedValueProperty.setValue(saveOrderConfig.getSortCriteria().get(1).field);
        saveSecDescPropertySelected.setValue(saveOrderConfig.getSortCriteria().get(1).descending);
        saveTerSortSelectedValueProperty.setValue(saveOrderConfig.getSortCriteria().get(2).field);
        saveTerDescPropertySelected.setValue(saveOrderConfig.getSortCriteria().get(2).descending);
    }

    private String getSelectedItemAsLowerCaseTrim(StringProperty string) {
        return string.getValue().toLowerCase(Locale.ROOT).trim();
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

    public StringProperty savePriSortSelectedValueProperty() {
        return savePriSortSelectedValueProperty;
    }

    public StringProperty saveSecSortSelectedValueProperty() {
        return saveSecSortSelectedValueProperty;
    }

    public StringProperty saveTerSortSelectedValueProperty() {
        return saveTerSortSelectedValueProperty;
    }

    public void storeConfig() {
        metadata.setSaveOrderConfig(this.getSaveOrderConfig());
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
