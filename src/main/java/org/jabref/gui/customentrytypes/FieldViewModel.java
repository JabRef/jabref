package org.jabref.gui.customentrytypes;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.customentrytypes.CustomEntryTypeDialogViewModel.FieldType;
import org.jabref.model.entry.field.Field;

public class FieldViewModel {

    private final ObjectProperty<FieldType> fieldTypeProperty = new SimpleObjectProperty<>();
    private final StringProperty fieldNameProperty = new SimpleStringProperty("");
    private final Field field;

    public FieldViewModel(Field field, FieldType fieldType) {
        this.field = field;
        this.fieldNameProperty.setValue(field.getDisplayName());
        this.fieldTypeProperty.setValue(fieldType);
    }

    public FieldViewModel(Field field, boolean required) {
        this.field = field;

        this.fieldNameProperty.setValue(field.getDisplayName());
        this.fieldTypeProperty.setValue(required ? FieldType.REQUIRED : FieldType.OTPIONAL);
    }

    public ObjectProperty<FieldType> fieldTypeProperty() {
        return this.fieldTypeProperty;
    }

    public StringProperty fieldNameProperty() {
        return this.fieldNameProperty;
    }

    public Field getField() {
        return this.field;
    }
}
