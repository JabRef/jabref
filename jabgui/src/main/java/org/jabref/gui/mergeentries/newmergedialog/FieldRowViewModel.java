package org.jabref.gui.mergeentries.newmergedialog;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.mergeentries.newmergedialog.fieldsmerger.FieldMerger;
import org.jabref.gui.mergeentries.newmergedialog.fieldsmerger.FieldMergerFactory;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.strings.StringUtil;

import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldRowViewModel {
    public enum Selection {
        LEFT,
        RIGHT,
        /**
         * When the user types something into the merged field value and neither the left nor
         * right values match it, NONE is selected
         * */
        NONE
    }

    private final Logger LOGGER = LoggerFactory.getLogger(FieldRowViewModel.class);
    private final BooleanProperty isFieldsMerged = new SimpleBooleanProperty(Boolean.FALSE);

    private final ObjectProperty<Selection> selection = new SimpleObjectProperty<>();

    private final StringProperty leftFieldValue = new SimpleStringProperty("");
    private final StringProperty rightFieldValue = new SimpleStringProperty("");
    private final StringProperty mergedFieldValue = new SimpleStringProperty("");

    private final Field field;

    private final BibEntry leftEntry;

    private final BibEntry rightEntry;

    private final BibEntry mergedEntry;

    private final BooleanBinding hasEqualLeftAndRight;

    private final FieldMergerFactory fieldMergerFactory;

    private final CompoundEdit fieldsMergedEdit = new CompoundEdit();

    public FieldRowViewModel(Field field, BibEntry leftEntry, BibEntry rightEntry, BibEntry mergedEntry, FieldMergerFactory fieldMergerFactory) {
        this.field = field;
        this.leftEntry = leftEntry;
        this.rightEntry = rightEntry;
        this.mergedEntry = mergedEntry;
        this.fieldMergerFactory = fieldMergerFactory;

        if (field.equals(InternalField.TYPE_HEADER)) {
            setLeftFieldValue(leftEntry.getType().getDisplayName());
            setRightFieldValue(rightEntry.getType().getDisplayName());
        } else {
            setLeftFieldValue(leftEntry.getField(field).orElse(""));
            setRightFieldValue(rightEntry.getField(field).orElse(""));
        }

        EasyBind.listen(leftFieldValueProperty(), (obs, old, leftValue) -> leftEntry.setField(field, leftValue));
        EasyBind.listen(rightFieldValueProperty(), (obs, old, rightValue) -> rightEntry.setField(field, rightValue));
        EasyBind.listen(mergedFieldValueProperty(), (obs, old, mergedFieldValue) -> {
            if (field.equals(InternalField.TYPE_HEADER)) {
                getMergedEntry().setType(EntryTypeFactory.parse(mergedFieldValue));
            } else {
                getMergedEntry().setField(field, mergedFieldValue);
            }
        });

        hasEqualLeftAndRight = Bindings.createBooleanBinding(this::hasEqualLeftAndRightValues, leftFieldValueProperty(), rightFieldValueProperty());

        selectNonEmptyValue();

        EasyBind.listen(isFieldsMergedProperty(), (obs, old, areFieldsMerged) -> {
            LOGGER.debug("Field are merged: {}", areFieldsMerged);
            if (areFieldsMerged) {
                selectLeftValue();
            } else {
                selectNonEmptyValue();
            }
        });

        EasyBind.subscribe(selectionProperty(), selection -> {
            LOGGER.debug("Selecting {}' value for field {}", selection, field.getDisplayName());
            switch (selection) {
                case LEFT -> EasyBind.subscribe(leftFieldValueProperty(), this::setMergedFieldValue);
                case RIGHT -> EasyBind.subscribe(rightFieldValueProperty(), this::setMergedFieldValue);
            }
        });

        EasyBind.subscribe(mergedFieldValueProperty(), mergedValue -> {
            LOGGER.debug("Merged value is {} for field {}", mergedValue, field.getDisplayName());
            if (mergedValue.equals(getLeftFieldValue())) {
                selectLeftValue();
            } else if (getMergedFieldValue().equals(getRightFieldValue())) {
                selectRightValue();
            } else {
                selectNone();
            }
        });

        EasyBind.subscribe(hasEqualLeftAndRightBinding(), this::setIsFieldsMerged);
    }

    public void selectNonEmptyValue() {
        if (StringUtil.isNullOrEmpty(leftFieldValue.get())) {
            selectRightValue();
        } else {
            selectLeftValue();
        }
    }

    public boolean hasEqualLeftAndRightValues() {
        return leftFieldValue.get().equals(rightFieldValue.get());
    }

    public void selectLeftValue() {
        setSelection(Selection.LEFT);
    }

    public void selectRightValue() {
        if (isFieldsMerged()) {
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

    public void mergeFields() {
        assert !hasEqualLeftAndRightValues();

        if (!FieldMergerFactory.canMerge(field)) {
            throw new UnsupportedOperationException();
        }

        String oldLeftFieldValue = getLeftFieldValue();
        String oldRightFieldValue = getRightFieldValue();

        FieldMerger fieldMerger = fieldMergerFactory.create(field);
        String mergedFields = fieldMerger.merge(getLeftFieldValue(), getRightFieldValue());
        setLeftFieldValue(mergedFields);
        setRightFieldValue(mergedFields);

        if (fieldsMergedEdit.canRedo()) {
            fieldsMergedEdit.redo();
        } else {
            fieldsMergedEdit.addEdit(new MergeFieldsUndo(oldLeftFieldValue, oldRightFieldValue, mergedFields));
            fieldsMergedEdit.end();
        }
    }

    public void unmergeFields() {
        if (fieldsMergedEdit.canUndo()) {
            fieldsMergedEdit.undo();
        }
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

    public boolean isFieldsMerged() {
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

    public Field getField() {
        return field;
    }

    public BibEntry getLeftEntry() {
        return leftEntry;
    }

    public BibEntry getRightEntry() {
        return rightEntry;
    }

    public BibEntry getMergedEntry() {
        return mergedEntry;
    }

    class MergeFieldsUndo extends AbstractUndoableEdit {
        private final String oldLeft;
        private final String oldRight;
        private final String mergedFields;

        MergeFieldsUndo(String oldLeft, String oldRight, String mergedFields) {
            this.oldLeft = oldLeft;
            this.oldRight = oldRight;
            this.mergedFields = mergedFields;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            setLeftFieldValue(oldLeft);
            setRightFieldValue(oldRight);
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            setLeftFieldValue(mergedFields);
            setRightFieldValue(mergedFields);
        }
    }
}
