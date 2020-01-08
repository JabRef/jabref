package org.jabref.gui.customentrytypes;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.customentrytypes.CustomEntryTypeDialogViewModel.FieldType;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.Field;

public class FieldViewModel {

    private final ObjectProperty<FieldType> fieldTypeProperty = new SimpleObjectProperty<>();
    private final StringProperty fieldNameProperty = new SimpleStringProperty("");
    private final Field field;
    private BibEntryType entryType;

    public FieldViewModel(Field field, FieldType fieldType, BibEntryType entryType) {
        this.field = field;
        this.entryType = entryType;
        this.fieldNameProperty.setValue(field.getDisplayName());
        this.fieldTypeProperty.setValue(fieldType);
    }

    public FieldViewModel(Field field, boolean required, BibEntryType entryType) {
        this(field, required ? FieldType.REQUIRED : FieldType.OTPIONAL, entryType);
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

    public BibEntryType getEntryType() {
        return entryType;

    }
}
