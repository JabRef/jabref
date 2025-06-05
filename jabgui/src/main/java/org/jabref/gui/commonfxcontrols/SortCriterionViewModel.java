package org.jabref.gui.commonfxcontrols;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.SaveOrder;

public class SortCriterionViewModel {

    private final ObjectProperty<Field> fieldProperty = new SimpleObjectProperty<>();
    private final BooleanProperty descendingProperty = new SimpleBooleanProperty();

    public SortCriterionViewModel(SaveOrder.SortCriterion criterion) {
        this.fieldProperty.setValue(criterion.field);
        this.descendingProperty.setValue(criterion.descending);
    }

    public SortCriterionViewModel() {
        this.fieldProperty.setValue(StandardField.AUTHOR);
        this.descendingProperty.setValue(false);
    }

    public ObjectProperty<Field> fieldProperty() {
        return fieldProperty;
    }

    public BooleanProperty descendingProperty() {
        return descendingProperty;
    }

    public SaveOrder.SortCriterion getCriterion() {
        return new SaveOrder.SortCriterion(fieldProperty.getValue(), descendingProperty.getValue());
    }
}
