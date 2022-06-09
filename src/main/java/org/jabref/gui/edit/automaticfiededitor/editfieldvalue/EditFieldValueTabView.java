package org.jabref.gui.edit.automaticfiededitor.editfieldvalue;

import java.util.List;

import javax.inject.Inject;
import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabView;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.views.ViewLoader;

public class EditFieldValueTabView extends AbstractAutomaticFieldEditorTabView {
    @FXML
    private ComboBox<String> fieldComboBox;

    @FXML
    private TextField fieldValueTextField;

    @FXML
    private CheckBox overwriteNonEmptyFieldsCheckBox;

    @FXML
    private Button setValueButton;

    @Inject private UndoManager undoManager;

    private final List<BibEntry> selectedEntries;
    private final BibDatabaseContext databaseContext;

    private EditFieldValueViewModel viewModel;

    public EditFieldValueTabView(List<BibEntry> selectedEntries, BibDatabaseContext databaseContext) {
        this.selectedEntries = selectedEntries;
        this.databaseContext = databaseContext;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    public void initialize() {
        viewModel = new EditFieldValueViewModel(databaseContext, selectedEntries, undoManager);
        fieldComboBox.getItems().addAll(viewModel.getAllFieldNames());
    }

    @Override
    public String getTabName() {
        return Localization.lang("Edit field value");
    }

    @FXML
    void appendToFieldValue() {
        viewModel.appendToFieldValue();
    }

    @FXML
    void clearField() {
        viewModel.clearSelectedField();
    }

    @FXML
    void setFieldValue() {
        viewModel.setFieldValue();
    }
}
