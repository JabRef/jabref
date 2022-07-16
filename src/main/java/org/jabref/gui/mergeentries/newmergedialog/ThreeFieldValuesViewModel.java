package org.jabref.gui.mergeentries.newmergedialog;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.strings.StringUtil;

public class ThreeFieldValuesViewModel {
    public enum Selection {
        LEFT, RIGHT, MERGED
    }

    private final ObjectProperty<Selection> selectedCell = new SimpleObjectProperty<>();

    private final StringProperty leftFieldValue = new SimpleStringProperty();
    private final StringProperty rightFieldValue = new SimpleStringProperty();
    private final StringProperty mergedFieldValue = new SimpleStringProperty();

    private final BooleanBinding hasEqualLeftAndRight;

    public ThreeFieldValuesViewModel() {
        hasEqualLeftAndRight = Bindings.createBooleanBinding(this::hasEqualLeftAndRightValues, leftFieldValue, rightFieldValue);
    }

    public boolean hasEqualLeftAndRightValues() {
        return (!StringUtil.isNullOrEmpty(leftFieldValue.get()) &&
                !StringUtil.isNullOrEmpty(rightFieldValue.get()) &&
                leftFieldValue.get().equals(rightFieldValue.get()));
    }

    public void selectLeftValue() {
        selectedCell.set(Selection.LEFT);
        mergedFieldValue.set(leftFieldValue.getValue());
    }

    public void merge() {
        selectedCell.set(Selection.MERGED);
    }

    public BooleanBinding hasEqualLeftAndRightBinding() {
        return hasEqualLeftAndRight;
    }
}
