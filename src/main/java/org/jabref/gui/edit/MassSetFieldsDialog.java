package org.jabref.gui.edit;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.strings.StringUtil;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.Severity;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.Validator;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class MassSetFieldsDialog extends BaseDialog<Void> {

    private final List<BibEntry> entries;
    private final BibDatabaseContext database;
    private final DialogService dialogService;

    private RadioButton clearRadioButton;
    private RadioButton setRadioButton;
    private RadioButton appendRadioButton;
    private RadioButton renameRadioButton;
    private ComboBox<String> fieldComboBox;
    private TextField setTextField;
    private TextField appendTextField;
    private TextField renameTextField;
    private CheckBox overwriteCheckBox;
    private UndoManager undoManager;

    MassSetFieldsDialog(List<BibEntry> entries, BibDatabaseContext database, DialogService dialogService, UndoManager undoManager) {
        this.entries = entries;
        this.database = database;
        this.dialogService = dialogService;
        this.undoManager = undoManager;

        init();
        this.setTitle(Localization.lang("Manage field names & content"));
        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        this.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                performEdits();
            }
            return null;
        });
    }

    /**
     * Append a given value to a given field for all entries in a Collection. This method DOES NOT update any UndoManager,
     * but returns a relevant CompoundEdit that should be registered by the caller.
     *
     * @param entries      The entries to process the operation for.
     * @param field        The name of the field to append to.
     * @param textToAppend The value to set. A null in this case will simply preserve the current field state.
     * @return A CompoundEdit for the entire operation.
     */
    private static UndoableEdit massAppendField(Collection<BibEntry> entries, Field field, String textToAppend) {
        String newValue = "";

        if (textToAppend != null) {
            newValue = textToAppend;
        }

        NamedCompound compoundEdit = new NamedCompound(Localization.lang("Append field"));
        for (BibEntry entry : entries) {
            Optional<String> oldValue = entry.getField(field);
            entry.setField(field, oldValue.orElse("") + newValue);
            compoundEdit.addEdit(new UndoableFieldChange(entry, field, oldValue.orElse(null), newValue));
        }
        compoundEdit.end();
        return compoundEdit;
    }

    /**
     * Move contents from one field to another for a Collection of entries.
     *
     * @param entries         The entries to do this operation for.
     * @param field           The field to move contents from.
     * @param newField        The field to move contents into.
     * @param overwriteValues If true, overwrites any existing values in the new field. If false, makes no change for
     *                        entries with existing value in the new field.
     * @return A CompoundEdit for the entire operation.
     */
    private static UndoableEdit massRenameField(Collection<BibEntry> entries, Field field, Field newField,
                                                boolean overwriteValues) {
        NamedCompound compoundEdit = new NamedCompound(Localization.lang("Rename field"));
        for (BibEntry entry : entries) {
            Optional<String> valToMove = entry.getField(field);
            // If there is no value, do nothing:
            if ((!valToMove.isPresent()) || valToMove.get().isEmpty()) {
                continue;
            }
            // If we are not allowed to overwrite values, check if there is a
            // non-empty value already for this entry for the new field:
            Optional<String> valInNewField = entry.getField(newField);
            if (!overwriteValues && (valInNewField.isPresent()) && !valInNewField.get().isEmpty()) {
                continue;
            }

            entry.setField(newField, valToMove.get());
            compoundEdit.addEdit(new UndoableFieldChange(entry, newField, valInNewField.orElse(null), valToMove.get()));
            entry.clearField(field);
            compoundEdit.addEdit(new UndoableFieldChange(entry, field, valToMove.get(), null));
        }
        compoundEdit.end();
        return compoundEdit;
    }

    /**
     * Set a given field to a given value for all entries in a Collection. This method DOES NOT update any UndoManager,
     * but returns a relevant CompoundEdit that should be registered by the caller.
     *
     * @param entries         The entries to set the field for.
     * @param field           The name of the field to set.
     * @param textToSet       The value to set. This value can be null, indicating that the field should be cleared.
     * @param overwriteValues Indicate whether the value should be set even if an entry already has the field set.
     * @return A CompoundEdit for the entire operation.
     */
    private static UndoableEdit massSetField(Collection<BibEntry> entries, Field field, String textToSet,
                                             boolean overwriteValues) {
        NamedCompound compoundEdit = new NamedCompound(Localization.lang("Set field"));
        for (BibEntry entry : entries) {
            Optional<String> oldValue = entry.getField(field);
            // If we are not allowed to overwrite values, check if there is a
            // nonempty
            // value already for this entry:
            if (!overwriteValues && (oldValue.isPresent()) && !oldValue.get().isEmpty()) {
                continue;
            }
            if (textToSet == null) {
                entry.clearField(field);
            } else {
                entry.setField(field, textToSet);
            }
            compoundEdit.addEdit(new UndoableFieldChange(entry, field, oldValue.orElse(null), textToSet));
        }
        compoundEdit.end();
        return compoundEdit;
    }

    private void init() {
        fieldComboBox = new ComboBox<>();
        fieldComboBox.setEditable(true);
        fieldComboBox.getItems().addAll(database.getDatabase().getAllVisibleFields().stream().map(Field::getName).collect(Collectors.toSet()));

        ToggleGroup toggleGroup = new ToggleGroup();
        clearRadioButton = new RadioButton(Localization.lang("Clear fields"));
        clearRadioButton.setToggleGroup(toggleGroup);
        renameRadioButton = new RadioButton(Localization.lang("Rename field to") + ":");
        renameRadioButton.setTooltip(new Tooltip(Localization.lang("Move contents of a field into a field with a different name")));
        renameRadioButton.setToggleGroup(toggleGroup);
        setRadioButton = new RadioButton(Localization.lang("Set fields") + ":");
        setRadioButton.setToggleGroup(toggleGroup);
        appendRadioButton = new RadioButton(Localization.lang("Append to fields") + ":");
        appendRadioButton.setToggleGroup(toggleGroup);

        setTextField = new TextField();
        setTextField.disableProperty().bind(setRadioButton.selectedProperty().not());
        appendTextField = new TextField();
        appendTextField.disableProperty().bind(appendRadioButton.selectedProperty().not());
        renameTextField = new TextField();
        renameTextField.disableProperty().bind(renameRadioButton.selectedProperty().not());

        overwriteCheckBox = new CheckBox(Localization.lang("Overwrite existing field values"));

        GridPane main = new GridPane();
        main.add(new Label(Localization.lang("Field name")), 0, 0);
        main.add(fieldComboBox, 1, 0);
        main.add(setRadioButton, 0, 2);
        main.add(setTextField, 1, 2);
        main.add(appendRadioButton, 0, 3);
        main.add(appendTextField, 1, 3);
        main.add(renameRadioButton, 0, 4);
        main.add(renameTextField, 1, 4);
        main.add(clearRadioButton, 0, 5);
        main.add(overwriteCheckBox, 0, 7);

        main.setPadding(new Insets(15, 15, 0, 15));
        main.setGridLinesVisible(false);
        main.setVgap(4);
        main.setHgap(10);
        getDialogPane().setContent(main);

        Validator fieldNameValidator = new FunctionBasedValidator<>(
                fieldComboBox.valueProperty(),
                StringUtil::isNotBlank,
                new ValidationMessage(Severity.ERROR, Localization.lang("You must enter at least one field name"))
        );
        Platform.runLater(() -> {
            // Need to run this async, otherwise the dialog does not work
            ControlsFxVisualizer visualizer = new ControlsFxVisualizer();
            visualizer.setDecoration(new IconValidationDecorator());
            visualizer.initVisualization(fieldNameValidator.getValidationStatus(), fieldComboBox, true);
        });
    }

    private void performEdits() {
        String toSet = setTextField.getText();
        if (toSet.isEmpty()) {
            toSet = null;
        }

        Field field = FieldFactory.parseField(fieldComboBox.getValue());

        NamedCompound compoundEdit = new NamedCompound(Localization.lang("Set field"));
        if (renameRadioButton.isSelected()) {
            compoundEdit.addEdit(massRenameField(entries, field, FieldFactory.parseField(renameTextField.getText()), overwriteCheckBox.isSelected()));
        } else if (appendRadioButton.isSelected()) {
            compoundEdit.addEdit(massAppendField(entries, field, appendTextField.getText()));
        } else {
            compoundEdit.addEdit(massSetField(entries, field,
                    setRadioButton.isSelected() ? toSet : null,
                    overwriteCheckBox.isSelected()));
        }
        compoundEdit.end();
        undoManager.addEdit(compoundEdit);
    }
}
