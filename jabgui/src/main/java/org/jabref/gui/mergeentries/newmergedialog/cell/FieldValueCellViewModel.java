package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.ToggleGroup;

public class FieldValueCellViewModel {
    private final StringProperty fieldValue = new SimpleStringProperty();
    private final BooleanProperty selected = new SimpleBooleanProperty(FieldValueCell.class, "selected");
    private final ObjectProperty<ToggleGroup> toggleGroup = new SimpleObjectProperty<>();

    public FieldValueCellViewModel(String text) {
        setFieldValue(text);
    }

    public String getFieldValue() {
        return fieldValue.get();
    }

    public StringProperty fieldValueProperty() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue.set(fieldValue);
    }

    public boolean isSelected() {
        return selected.get();
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public ToggleGroup getToggleGroup() {
        return toggleGroup.get();
    }

    public ObjectProperty<ToggleGroup> toggleGroupProperty() {
        return toggleGroup;
    }

    public void setToggleGroup(ToggleGroup toggleGroup) {
        this.toggleGroup.set(toggleGroup);
    }
}
