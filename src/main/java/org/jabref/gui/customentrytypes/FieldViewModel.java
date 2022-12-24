package org.jabref.gui.customentrytypes;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.customentrytypes.CustomEntryTypeDialogViewModel.FieldType;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.FieldProperty;

import com.tobiasdiez.easybind.EasyBind;

public class FieldViewModel {

    private final ObjectProperty<FieldType> fieldType;
    private final StringProperty fieldName = new SimpleStringProperty("");
    private final Field field;
    private final FieldPriority fieldPriority;
    private final BooleanProperty multiline = new SimpleBooleanProperty();

    public FieldViewModel(Field field, FieldType fieldType, FieldPriority fieldPriority, boolean multiline) {
        this.field = field;
        this.fieldName.setValue(field.getDisplayName());
        this.fieldType = new SimpleObjectProperty<>(fieldType);
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

    public ObjectProperty<FieldType> fieldType() {
        return this.fieldType;
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
        return this.fieldType.getValue();
    }

    public void setFieldType(FieldType type) {
        this.fieldType.setValue(type);
    }

    public BooleanProperty multiline() {
        return this.multiline;
    }

    @Override
    public String toString() {
        return this.field.getDisplayName();
    }
}
