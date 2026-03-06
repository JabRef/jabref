package org.jabref.gui.preferences.customentrytypes;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;

public class FieldViewModel {

    private final StringProperty displayName = new SimpleStringProperty("");
    private final BooleanProperty required = new SimpleBooleanProperty();
    private final BooleanProperty multiline = new SimpleBooleanProperty();
    private final ObjectProperty<FieldPriority> priorityProperty = new SimpleObjectProperty<>();
    private final ObservableList<FieldProperty> properties = FXCollections.observableArrayList();

    public FieldViewModel(Field field,
                          Mandatory required,
                          FieldPriority priorityProperty,
                          boolean multiline) {
        this.displayName.setValue(FieldTextMapper.getDisplayName(field));
        this.required.setValue(required == Mandatory.REQUIRED);
        this.priorityProperty.setValue(priorityProperty);
        this.multiline.setValue(multiline);
        field.getProperties()
             .stream()
             .filter(property -> property != FieldProperty.MULTILINE_TEXT)
             .forEach(properties::add);
    }

    public StringProperty displayNameProperty() {
        return displayName;
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

    public ObservableList<FieldProperty> getProperties() {
        return properties;
    }

    public Field toField(EntryType type) {
        // If the field name is known by JabRef, JabRef's casing will win.
        // If the field is not known by JabRef (UnknownField), the new casing will be taken.
        Field field = FieldFactory.parseField(type, displayName.getValue());
        if (field instanceof UnknownField) {
            field.getProperties().addAll(properties);
        }
        if (multiline.getValue()) {
            field.getProperties().add(FieldProperty.MULTILINE_TEXT);
        }
        return field;
    }

    public BibField toBibField(EntryType type) {
        return new BibField(toField(type), priorityProperty.getValue());
    }

    @Override
    public String toString() {
        return displayName.getValue();
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
