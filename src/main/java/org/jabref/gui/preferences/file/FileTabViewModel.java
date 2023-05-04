package org.jabref.gui.preferences.file;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.entry.field.FieldFactory;

public class FileTabViewModel implements PreferenceTabViewModel {
    private final BooleanProperty resolveStringsProperty = new SimpleBooleanProperty();
    private final StringProperty resolveStringsForFieldsProperty = new SimpleStringProperty("");
    private final StringProperty nonWrappableFieldsProperty = new SimpleStringProperty("");

    private final FieldPreferences fieldPreferences;

    FileTabViewModel(FieldPreferences fieldPreferences) {
        this.fieldPreferences = fieldPreferences;
    }

    @Override
    public void setValues() {
        resolveStringsProperty.setValue(fieldPreferences.shouldResolveStrings());
        resolveStringsForFieldsProperty.setValue(FieldFactory.serializeFieldsList(fieldPreferences.getResolvableFields()));
        nonWrappableFieldsProperty.setValue(FieldFactory.serializeFieldsList(fieldPreferences.getNonWrappableFields()));
    }

    @Override
    public void storeSettings() {
        fieldPreferences.setResolveStrings(resolveStringsProperty.getValue());
        fieldPreferences.setResolvableFields(FieldFactory.parseFieldList(resolveStringsForFieldsProperty.getValue().trim()));
        fieldPreferences.setNonWrappableFields(FieldFactory.parseFieldList(nonWrappableFieldsProperty.getValue().trim()));
    }

    public BooleanProperty resolveStringsProperty() {
        return resolveStringsProperty;
    }

    public StringProperty resolveStringsForFieldsProperty() {
        return resolveStringsForFieldsProperty;
    }

    public StringProperty nonWrappableFieldsProperty() {
        return nonWrappableFieldsProperty;
    }
}
