package org.jabref.gui.mergeentries.newmergedialog;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.strings.StringUtil;

public class ThreeFieldValuesViewModel {
    public enum Selection {
        LEFT,
        RIGHT,
        /**
         * When the user types something into the merged field value and neither the left nor
         * right values match it, NONE is selected
         * */
        NONE
    }


    private final BooleanProperty isFieldsMerged = new SimpleBooleanProperty(Boolean.FALSE);

    private final ObjectProperty<Selection> selection = new SimpleObjectProperty<>();

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
        setSelection(Selection.LEFT);
        setMergedFieldValue(getLeftFieldValue());
    }

    public void selectRightValue() {
        if (isIsFieldsMerged()) {
            selectLeftValue();
        } else {
            setSelection(Selection.RIGHT);
            setMergedFieldValue(getRightFieldValue());
        }
    }

    public void setMergedFieldValue(String mergedFieldValue) {
        mergedFieldValueProperty().set(mergedFieldValue);
    }

    public StringProperty mergedFieldValueProperty() {
        return mergedFieldValue;
    }

    public void merge() {
        setIsFieldsMerged(true);
    }

    public BooleanBinding hasEqualLeftAndRightBinding() {
        return hasEqualLeftAndRight;
    }

    public ObjectProperty<Selection> selectionProperty() {
        return selection;
    }

    public void setSelection(Selection select) {
        selectionProperty().set(select);
    }

    public Selection getSelection() {
        return selectionProperty().get();
    }

    public boolean isIsFieldsMerged() {
        return isFieldsMerged.get();
    }

    public BooleanProperty isFieldsMergedProperty() {
        return isFieldsMerged;
    }

    public void setIsFieldsMerged(boolean isFieldsMerged) {
        this.isFieldsMerged.set(isFieldsMerged);
    }

    public String getLeftFieldValue() {
        return leftFieldValue.get();
    }

    public StringProperty leftFieldValueProperty() {
        return leftFieldValue;
    }

    public void setLeftFieldValue(String leftFieldValue) {
        this.leftFieldValue.set(leftFieldValue);
    }

    public String getRightFieldValue() {
        return rightFieldValue.get();
    }

    public StringProperty rightFieldValueProperty() {
        return rightFieldValue;
    }

    public void setRightFieldValue(String rightFieldValue) {
        this.rightFieldValue.set(rightFieldValue);
    }
}
