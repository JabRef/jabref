package org.jabref.gui.actions;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.undo.UndoableEdit;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import org.jabref.gui.BasePanel;
import org.jabref.gui.FXDialog;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
/**
 * An Action for launching mass field.
 *
 * Functionality:
 * * Defaults to selected entries, or all entries if none are selected.
 * * Input field name
 * * Either set field, or clear field.
 */
public class MassSetFieldAction extends SimpleCommand {

    public javafx.scene.control.Dialog FXDialog = new javafx.scene.control.Dialog();
    private final JabRefFrame frame;
    private FXDialog diag;
    private RadioButton all;
    private RadioButton selected;
    private RadioButton clear;
    private RadioButton set;
    private RadioButton append;
    private RadioButton rename;
    private ComboBox<String> field;
    private TextField textFieldSet;
    private TextField textFieldAppend;
    private TextField textFieldRename;
    private boolean canceled = true;
    private CheckBox overwrite;


    public MassSetFieldAction(JabRefFrame frame) {
        //super(AlertType.NONE, Localization.lang("Set/clear/append/rename fields"), true);
        this.frame = frame;
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
    private static UndoableEdit massSetField(Collection<BibEntry> entries, String field, String textToSet,
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

    private void prepareDialog(boolean selection) {

        //selected.setEnabled(selection);
        if (selection) {
            selected.setSelected(true);
        } else {
            all.setSelected(true);
        }
        // Make sure one of the following ones is selected:
        if (!set.isSelected() && !clear.isSelected() && !rename.isSelected()) {
            set.setSelected(true);
        }
    }


    @Override
    public void execute() {

        BasePanel bp = frame.getCurrentBasePanel();
        if (bp == null) {
            return;
        }
        List<BibEntry> entries = bp.getSelectedEntries();
        // Lazy creation of the dialog:
        if (diag == null) {
            createDialog();
        }
        canceled = true;
        prepareDialog(!entries.isEmpty());

        if (canceled) {
            return;
        }

        Collection<BibEntry> entryList;
        // If all entries should be treated, change the entries array:
        if (all.isSelected()) {
            entryList = bp.getDatabase().getEntries();
        } else {
            entryList = entries;
        }

        String toSet = textFieldSet.getText();
        if (toSet.isEmpty()) {
            toSet = null;
        }

        String[] fields = (String[]) field.getItems().toArray();
        for (String s : fields) {
            s = s.toLowerCase();
        }

        NamedCompound compoundEdit = new NamedCompound(Localization.lang("Set field"));
        if (rename.isSelected()) {
            if (fields.length > 1) {
                frame.getDialogService().showErrorDialogAndWait(Localization.lang("You can only rename one field at a time"));
                return; // Do not close the dialog.
            } else {
                compoundEdit.addEdit(MassSetFieldAction.massRenameField(entryList, fields[0], textFieldRename.getText(),
                        overwrite.isSelected()));
            }
        } else if (append.isSelected()) {
            for (String field : fields) {
                compoundEdit.addEdit(MassSetFieldAction.massAppendField(entryList, field, textFieldAppend.getText()));
            }
        } else {
            for (String field : fields) {
                compoundEdit.addEdit(MassSetFieldAction.massSetField(entryList, field,
                        set.isSelected() ? toSet : null,
                        overwrite.isSelected()));
            }
        }
        compoundEdit.end();
        bp.getUndoManager().addEdit(compoundEdit);
        bp.markBaseChanged();
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
    private static UndoableEdit massAppendField(Collection<BibEntry> entries, String field, String textToAppend) {

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
    private static UndoableEdit massRenameField(Collection<BibEntry> entries, String field, String newField,
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

    private void createDialog() {
        diag = new FXDialog(AlertType.NONE, Localization.lang("Set/clear/append/rename fields"), true);
        FXDialog.setTitle("Set/clear/append/rename fields");


        field = new ComboBox<>();
        field.setEditable(true);
        textFieldSet = new TextField();

        textFieldSet.setDisable(true);
        textFieldAppend = new TextField();

        textFieldAppend.setDisable(true);
        textFieldRename = new TextField();

        textFieldRename.setDisable(true);


        Button ok = new Button(Localization.lang("OK"));
        Button cancel = new Button(Localization.lang("Cancel"));
        ok.setOnAction(e -> {
            // Check that any field name is set

            String fieldText = field.getItems().toString();
            if ((fieldText == null) || fieldText.trim().isEmpty()) {

                frame.getDialogService().showErrorDialogAndWait(Localization.lang("You must enter at least one field name"));

                return; // Do not close the dialog.
            }

            // Check if the user tries to rename multiple fields:
            if (rename.isSelected()) {
                String[] fields = getFieldNames(fieldText);
                if (fields.length > 1) {

                    frame.getDialogService().showErrorDialogAndWait(Localization.lang("You can only rename one field at a time"));

                    return; // Do not close the dialog.
                }
            }
            canceled = false;
            dispose();
        });

        cancel.setText(Localization.lang("Cancel"));
        cancel.setOnAction(oa -> dispose());
        cancel.setPrefWidth(100);
        cancel.setPrefHeight(30);

        all = new RadioButton(Localization.lang("All entries"));
        selected = new RadioButton(Localization.lang("Selected entries"));
        clear = new RadioButton(Localization.lang("Clear fields"));
        set = new RadioButton(Localization.lang("Set fields"));
        append = new RadioButton(Localization.lang("Append to fields"));
        rename = new RadioButton(Localization.lang("Rename field to") + ":");
        Tooltip aaa = new Tooltip(Localization.lang("Move contents of a field into a field with a different name"));
        rename.setTooltip(aaa);

        Set<String> allFields = frame.getCurrentBasePanel().getDatabase().getAllVisibleFields();

        for (String f : allFields) {
            //field.addItem(f);
            field.getItems().add(f);

        }
        set.selectedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean a,Boolean b) {
                textFieldSet.setDisable(!set.isSelected());
            }
        });
        append.selectedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean a,Boolean b) {
                textFieldSet.setDisable(!(!clear.isSelected() && !append.isSelected()));
            }
        });
        clear.selectedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean a,Boolean b) {
                textFieldSet.setDisable(!(!clear.isSelected() && !append.isSelected()));
            }
        });
        rename.selectedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean a,Boolean b) {
                textFieldSet.setDisable(!rename.isSelected());
            }
        });

        overwrite = new CheckBox("Overwrite existing field values");

        GridPane main=new GridPane();
        GridPane fn = new GridPane();
        GridPane ie = new GridPane();
        GridPane nfv = new GridPane();
        HBox title1 = new HBox();
        HBox title2 = new HBox();
        HBox title3 = new HBox();
        HBox optionButtons = new HBox();
        FXDialog.getDialogPane().setContent(main);
        main.setPrefSize(400, 400);

        main.add(title1, 0, 0);
        main.add(fn, 0, 1);
        main.add(title2, 0, 2);
        main.add(ie, 0, 3, 2, 1);
        main.add(title3, 0, 5);
        main.add(nfv, 0, 6, 4, 1);
        main.add(optionButtons, 0, 10);
        main.setPadding(new Insets(15, 5, 0, 0));
        main.setGridLinesVisible(false);

        Label str1 = new Label(Localization.lang("Field name"));
        Label str2 = new Label(Localization.lang("Include entries"));
        Label str3 = new Label(Localization.lang("New field value"));
        title1.getChildren().add(str1);
        title1.setPadding(new Insets(10, 0, 0, 0));
        title1.setAlignment(Pos.CENTER_LEFT);
        title2.getChildren().add(str2);
        title2.setPadding(new Insets(10, 0, 0, 0));
        title2.setAlignment(Pos.CENTER_LEFT);
        title3.getChildren().add(str3);
        title3.setPadding(new Insets(10, 0, 0, 0));
        title3.setAlignment(Pos.CENTER_LEFT);

        fn.setHgap(34);
        fn.setVgap(5);
        fn.setPadding(new Insets(5, 15, 5, 15));
        fn.add(new Label(Localization.lang("Field name") + ":"), 0, 0);
        fn.add(field, 1, 0);
        fn.setStyle("-fx-content-display:top;"
                    + "-fx-border-insets:0 0 0 0;"
                    + "-fx-border-color:#D3D3D3");

        ie.setHgap(10);
        ie.setVgap(10);
        ie.setPadding(new Insets(5, 15, 5, 15));
        ie.add(all, 0, 0);
        ie.add(selected, 0, 1);
        ie.setStyle("-fx-content-display:top;"
                    + "-fx-border-insets:0 0 0 0;"
                    + "-fx-border-color:#D3D3D3");

        nfv.setHgap(34);
        nfv.setVgap(5);
        nfv.setPadding(new Insets(5, 15, 5, 15));
        nfv.add(set, 0, 0);
        nfv.add(clear, 0, 1);
        nfv.add(append, 0, 2);
        nfv.add(rename, 0, 3);
        nfv.add(overwrite, 0, 4);
        nfv.setStyle("-fx-content-display:top;"
                    + "-fx-border-insets:0 0 0 0;"
                    + "-fx-border-color:#D3D3D3");

        optionButtons.setAlignment(Pos.BOTTOM_CENTER);
        optionButtons.setPadding(new Insets(15, 0, 0, 0));
        optionButtons.setSpacing(35);
        optionButtons.getChildren().add(ok);
        optionButtons.getChildren().add(cancel);

        FXDialog.showAndWait();

    }



    private static String[] getFieldNames(String s) {
        return s.split("[\\s;,]");
    }

    public void setVisible(boolean b) {
        if (b) {
            FXDialog.showAndWait();
        } else {
            FXDialog.hide();
        }
    }

    private void dispose() {

        ((Stage) (FXDialog.getDialogPane().getScene().getWindow())).close();
    }
}
