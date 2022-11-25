package org.jabref.gui.commonfxcontrols;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.model.entry.field.Field;
import org.jabref.model.metadata.SaveOrderConfig;

public class SortCriterionViewModel {

    private final ObjectProperty<Field> fieldProperty = new SimpleObjectProperty<>();
    private final BooleanProperty descendingProperty = new SimpleBooleanProperty();

    public SortCriterionViewModel(SaveOrderConfig.SortCriterion criterion) {
        this.fieldProperty.setValue(criterion.field);
        this.descendingProperty.setValue(criterion.descending);
    }

    public ObjectProperty<Field> fieldProperty() {
        return fieldProperty;
    }

    public BooleanProperty descendingProperty() {
        return descendingProperty;
    }

    public SaveOrderConfig.SortCriterion getCriterion() {
        return new SaveOrderConfig.SortCriterion(fieldProperty.getValue(), descendingProperty.getValue());
    }
}
