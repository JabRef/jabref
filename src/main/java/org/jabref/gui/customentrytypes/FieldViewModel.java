package org.jabref.gui.customentrytypes;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.customentrytypes.CustomEntryTypeDialogViewModel.FieldType;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldPriority;

public class FieldViewModel {

    private final ObjectProperty<FieldType> fieldType;
    private final StringProperty fieldName = new SimpleStringProperty("");
    private final Field field;
    private final FieldPriority fieldPriority;
    private BibEntryType entryType;

    public FieldViewModel(Field field, FieldType fieldType, FieldPriority fieldPriority, BibEntryType entryType) {
        this.field = field;
        this.entryType = entryType;
        this.fieldName.setValue(field.getDisplayName());
        this.fieldType = new SimpleObjectProperty<>(fieldType);
        this.fieldPriority = fieldPriority;
    }

    public FieldViewModel(Field field, boolean required, FieldPriority fieldPriority, BibEntryType entryType) {
        this(field, required ? FieldType.REQUIRED : FieldType.OPTIONAL, fieldPriority, entryType);
    }

    public ObjectProperty<FieldType> fieldType() {
        return this.fieldType;
    }

    public StringProperty fieldName() {
        return this.fieldName;
    }

    public Field getField() {
        return this.field;
    }

    public BibEntryType getEntryType() {
        return this.entryType;
    }

    public FieldPriority getFieldPriority() {
        return this.fieldPriority;
    }

    public FieldType getFieldType() {
        return this.fieldType.getValue();
    }

    public void setFieldType(FieldType type) {
        this.fieldType.setValue(type);
    }

    @Override
    public String toString() {
        return this.field.getDisplayName();
    }
}
