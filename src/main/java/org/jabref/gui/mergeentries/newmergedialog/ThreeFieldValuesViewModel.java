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

    private final StringProperty leftFieldValue = new SimpleStringProperty("");
    private final StringProperty rightFieldValue = new SimpleStringProperty("");
    private final StringProperty mergedFieldValue = new SimpleStringProperty("");

    private final BooleanBinding hasEqualLeftAndRight;

    public ThreeFieldValuesViewModel(String leftValue, String rightValue) {
        leftFieldValueProperty().set(leftValue);
        rightFieldValueProperty().set(rightValue);
        hasEqualLeftAndRight = Bindings.createBooleanBinding(this::hasEqualLeftAndRightValues, leftFieldValue, rightFieldValue);

        selectionProperty().addListener((obs, old, newVal) -> {
            switch (newVal) {
                case LEFT -> setMergedFieldValue(getLeftFieldValue());
                case RIGHT -> setMergedFieldValue(getRightFieldValue());
            }
        });

        mergedFieldValueProperty().addListener(obs -> {
            if (getMergedFieldValue().equals(getLeftFieldValue())) {
                selectLeftValue();
            } else if (getMergedFieldValue().equals(getRightFieldValue())) {
                selectRightValue();
            } else {
                selectNone();
            }
        });

        if (StringUtil.isNullOrEmpty(leftValue)) {
            selectRightValue();
        } else {
            selectLeftValue();
        }

        if (hasEqualLeftAndRight.get()) {
            setIsFieldsMerged(true);
        }

        hasEqualLeftAndRight.addListener(obs -> {
            setIsFieldsMerged(true);
        });
    }

    public boolean hasEqualLeftAndRightValues() {
        return (!StringUtil.isNullOrEmpty(leftFieldValue.get()) &&
                !StringUtil.isNullOrEmpty(rightFieldValue.get()) &&
                leftFieldValue.get().equals(rightFieldValue.get()));
    }

    public void selectLeftValue() {
        setSelection(Selection.LEFT);
    }

    public void selectRightValue() {
        if (isIsFieldsMerged()) {
            selectLeftValue();
        } else {
            setSelection(Selection.RIGHT);
        }
    }

    public void selectNone() {
        setSelection(Selection.NONE);
    }

    public void setMergedFieldValue(String mergedFieldValue) {
        mergedFieldValueProperty().set(mergedFieldValue);
    }

    public StringProperty mergedFieldValueProperty() {
        return mergedFieldValue;
    }

    public String getMergedFieldValue() {
        return mergedFieldValue.get();
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
