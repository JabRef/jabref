package org.jabref.gui.edit.automaticfiededitor.renamefield;

import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabView;
import org.jabref.gui.edit.automaticfiededitor.AutomaticFieldEditorTab;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import com.airhacks.afterburner.views.ViewLoader;

public class RenameFieldTabView extends AbstractAutomaticFieldEditorTabView implements AutomaticFieldEditorTab {
    @FXML
    private ComboBox<Field> fieldComboBox;
    @FXML
    private TextField newFieldNameTextField;
    private final List<BibEntry> selectedEntries;
    private final BibDatabase database;
    private final NamedCompound dialogEdits;
    private RenameFieldViewModel viewModel;

    public RenameFieldTabView(List<BibEntry> selectedEntries, BibDatabase database, NamedCompound dialogEdits) {
        this.selectedEntries = selectedEntries;
        this.database = database;
        this.dialogEdits = dialogEdits;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    public void initialize() {
        viewModel = new RenameFieldViewModel(selectedEntries, database, dialogEdits);

        fieldComboBox.getItems().setAll(viewModel.getAllFields());
        fieldComboBox.getSelectionModel().selectFirst();

        fieldComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Field field) {
                return field.getDisplayName();
            }

            @Override
            public Field fromString(String name) {
                return FieldFactory.parseField(name);
            }
        });

        fieldComboBox.valueProperty().bindBidirectional(viewModel.selectedFieldProperty());
        newFieldNameTextField.textProperty().bindBidirectional(viewModel.newFieldNameProperty());
    }

    @Override
    public String getTabName() {
        return Localization.lang("Rename field");
    }

    @Override
    public void registerListener(Object object) {
        viewModel.registerListener(object);
    }

    @Override
    public void unregisterListener(Object object) {
        viewModel.registerListener(object);
    }

    @FXML
    void renameField() {
        viewModel.renameField();
    }
}
