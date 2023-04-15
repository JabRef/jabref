package org.jabref.gui.preferences.customentrytypes;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.FieldProperty;

import com.tobiasdiez.easybind.EasyBind;

public class FieldViewModel {

    private final Field field;
    private final StringProperty fieldName = new SimpleStringProperty("");
    private final BooleanProperty required = new SimpleBooleanProperty();
    private final BooleanProperty multiline = new SimpleBooleanProperty();
    private final ObjectProperty<FieldPriority> priorityProperty = new SimpleObjectProperty<>();

    public FieldViewModel(Field field,
                          Mandatory required,
                          FieldPriority priorityProperty,
                          boolean multiline) {
        this.field = field;
        this.fieldName.setValue(field.getDisplayName());
        this.required.setValue(required == Mandatory.REQUIRED);
        this.priorityProperty.setValue(priorityProperty);
        this.multiline.setValue(multiline);

        EasyBind.subscribe(this.multiline, multi -> {
            if (multi) {
                this.field.getProperties().add(FieldProperty.MULTILINE_TEXT);
            } else {
                this.field.getProperties().remove(FieldProperty.MULTILINE_TEXT);
            }
        });
    }

    public Field getField() {
        return field;
    }

    public StringProperty nameProperty() {
        return fieldName;
    }

    public BooleanProperty requiredProperty() {
        return required;
    }

    public boolean isRequired() {
        return required.getValue();
    }

    public BooleanProperty multilineProperty() {
        return multiline;
    }

    public boolean isMultiline() {
        return multiline.getValue();
    }

    public FieldPriority getPriority() {
        return priorityProperty.getValue();
    }

    public BibField toBibField() {
        return new BibField(field, priorityProperty.getValue());
    }

    @Override
    public String toString() {
        return field.getDisplayName();
    }

    public enum Mandatory {

        REQUIRED(Localization.lang("Required")),
        OPTIONAL(Localization.lang("Optional"));

        private final String name;

        Mandatory(String name) {
            this.name = name;
        }

        public String getDisplayName() {
            return name;
        }
    }
}
