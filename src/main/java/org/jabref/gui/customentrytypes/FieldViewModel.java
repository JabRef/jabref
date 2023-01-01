package org.jabref.gui.customentrytypes;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.customentrytypes.CustomEntryTypeDialogViewModel.FieldType;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.FieldProperty;

import com.tobiasdiez.easybind.EasyBind;

public class FieldViewModel {

    private final BooleanProperty fieldTypeRequired = new SimpleBooleanProperty();
    private final StringProperty fieldName = new SimpleStringProperty("");
    private final Field field;
    private final FieldPriority fieldPriority;
    private final BooleanProperty multiline = new SimpleBooleanProperty();

    public FieldViewModel(Field field, FieldType fieldType, FieldPriority fieldPriority, boolean multiline) {
        this.field = field;
        this.fieldName.setValue(field.getDisplayName());
        this.fieldTypeRequired.setValue(FieldType.REQUIRED.equals(fieldType));
        this.fieldPriority = fieldPriority;
        this.multiline.setValue(multiline);

        EasyBind.subscribe(this.multiline, multi -> {
            if (multi) {
                this.field.getProperties().add(FieldProperty.MULTILINE_TEXT);
            } else {
                this.field.getProperties().remove(FieldProperty.MULTILINE_TEXT);
            }
        });
    }

    public FieldViewModel(Field field, boolean required, FieldPriority fieldPriority, boolean multiline) {
        this(field, required ? FieldType.REQUIRED : FieldType.OPTIONAL, fieldPriority, multiline);
    }

    public BooleanProperty fieldTypeRequired() {
        return this.fieldTypeRequired;
    }

    public StringProperty fieldName() {
        return this.fieldName;
    }

    public Field getField() {
        return this.field;
    }

    public FieldPriority getFieldPriority() {
        return this.fieldPriority;
    }

    public FieldType getFieldType() {
        return this.fieldTypeRequired.getValue() ? FieldType.REQUIRED : FieldType.OPTIONAL;
    }

    public BooleanProperty multiline() {
        return this.multiline;
    }

    @Override
    public String toString() {
        return this.field.getDisplayName();
    }
}
