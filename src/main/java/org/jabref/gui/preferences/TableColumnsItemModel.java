package org.jabref.gui.preferences;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.UnknownField;

public class TableColumnsItemModel {

    private final SimpleObjectProperty<Field> field;
    private final SimpleDoubleProperty length;
    private final ReadOnlyBooleanProperty editableProperty;

    public TableColumnsItemModel() {
        this.field = new SimpleObjectProperty<>(new UnknownField(Localization.lang("New column")));
        this.length = new SimpleDoubleProperty(ColumnPreferences.DEFAULT_FIELD_LENGTH);
        this.editableProperty = new SimpleBooleanProperty(true);
    }

    public TableColumnsItemModel(Field field) {
        this.field = new SimpleObjectProperty<>(field);
        this.length = new SimpleDoubleProperty(ColumnPreferences.DEFAULT_FIELD_LENGTH);
        this.editableProperty = new SimpleBooleanProperty(this.field.get() instanceof UnknownField);
    }

    public TableColumnsItemModel(Field field, double length) {
        this.field = new SimpleObjectProperty<>(field);
        this.length = new SimpleDoubleProperty(length);
        this.editableProperty = new SimpleBooleanProperty(this.field.get() instanceof UnknownField);
    }

    public Field getField() {
        return field.get();
    }

    public void setField(Field field) {
        this.field.set(field);
    }

    public String getName() {
        return field.get().getName();
    }

    public void setName(String name) {
        if (editableProperty.get()) {
            field.setValue(new UnknownField(name));
        }
    }

    public double getLength() {
        return length.get();
    }

    public void setLength(double length) {
        this.length.set(length);
    }

    public ReadOnlyBooleanProperty editableProperty() {
        return editableProperty;
    }

}
