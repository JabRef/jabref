package org.jabref.gui.preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.model.entry.field.Field;

public class XmpPrivacyItemModel {
    private ObjectProperty<Field> field;

    XmpPrivacyItemModel(Field field) {
        this.field = new SimpleObjectProperty<>(field);
    }

    public void setField(Field field) { this.field.setValue(field); }

    public Field getField() { return this.field.getValue(); }

    public ObjectProperty<Field> fieldProperty() {
        return field;
    }

    public String getName() { return field.getValue().getName();}

    @Override
    public String toString() {
        return field.getValue().getName();
    }
}
