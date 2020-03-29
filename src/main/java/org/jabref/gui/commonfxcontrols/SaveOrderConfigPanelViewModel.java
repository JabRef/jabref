package org.jabref.gui.commonfxcontrols;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.model.entry.field.Field;

public class SaveOrderConfigPanelViewModel {

    private final ListProperty<Field> primarySortFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Field> secondarySortFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Field> tertiarySortFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final BooleanProperty savePrimaryDescPropertySelected = new SimpleBooleanProperty();
    private final BooleanProperty saveSecondaryDescPropertySelected = new SimpleBooleanProperty();
    private final BooleanProperty saveTertiaryDescPropertySelected = new SimpleBooleanProperty();

    private final ObjectProperty<Field> savePrimarySortSelectedValueProperty = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Field> saveSecondarySortSelectedValueProperty = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Field> saveTertiarySortSelectedValueProperty = new SimpleObjectProperty<>(null);

    private final BooleanProperty saveInOriginalProperty = new SimpleBooleanProperty();
    private final BooleanProperty saveInTableOrderProperty = new SimpleBooleanProperty();
    private final BooleanProperty saveInSpecifiedOrderProperty = new SimpleBooleanProperty();

    public SaveOrderConfigPanelViewModel() {
    }

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
