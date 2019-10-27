package org.jabref.gui.preferences;

import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.UnknownField;

public class TableColumnsItemModel {

    private final ObjectProperty<Field> field;
    private final StringProperty name = new SimpleStringProperty("");
    private final DoubleProperty length = new SimpleDoubleProperty(ColumnPreferences.DEFAULT_FIELD_LENGTH);
    private final BooleanProperty editableProperty = new SimpleBooleanProperty(true);

    public TableColumnsItemModel() {
        this.field = new SimpleObjectProperty<>(new UnknownField(Localization.lang("New column")));
    }

    public TableColumnsItemModel(Field field) {
        this.field = new SimpleObjectProperty<>(field);
        this.editableProperty.setValue(this.field.get() instanceof UnknownField);
    }

    public TableColumnsItemModel(Field field, double length) {
        this.field = new SimpleObjectProperty<>(field);
        this.length.setValue(length);
        this.editableProperty.setValue(this.field.get() instanceof UnknownField);
    }

    public void setField(Field field) {
        this.field.set(field);
    }

    public Field getField() {
        return field.get();
    }

    public ObservableValue<Field> fieldProperty() { return this.field; }

    public void setName(String name) {
        if (editableProperty.get()) {
            field.setValue(new UnknownField(name));
        }
    }

    public String getName() {
        return field.get().getName();
    }

    public StringProperty nameProperty() { return this.name; }

    public void setLength(double length) {
        this.length.set(length);
    }

    public double getLength() {
        return length.get();
    }

    public DoubleProperty lengthProperty() { return this.length; }

    public ReadOnlyBooleanProperty editableProperty() { return editableProperty; }

    @Override
    public int hashCode() {
        return Objects.hash(field);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TableColumnsItemModel) {
            return Objects.equals(this.field, ((TableColumnsItemModel) obj).field);
        } else {
            return false;
        }
    }
}
