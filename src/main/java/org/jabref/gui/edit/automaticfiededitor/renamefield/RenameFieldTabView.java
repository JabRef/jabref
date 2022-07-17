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
import org.jabref.model.database.BibDatabaseContext;
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
    private final BibDatabaseContext databaseContext;
    private final NamedCompound dialogEdits;
    private RenameFieldViewModel viewModel;

    public RenameFieldTabView(List<BibEntry> selectedEntries, BibDatabaseContext databaseContext, NamedCompound dialogEdits) {
        this.selectedEntries = selectedEntries;
        this.databaseContext = databaseContext;
        this.dialogEdits = dialogEdits;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    public void initialize() {
        viewModel = new RenameFieldViewModel(selectedEntries, databaseContext, dialogEdits);

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

        viewModel.selectedFieldProperty().bindBidirectional(fieldComboBox.valueProperty());

        viewModel.newFieldNameProperty().bindBidirectional(newFieldNameTextField.textProperty());
    }

    @Override
    public String getTabName() {
        return Localization.lang("Rename field");
    }

    @FXML
    void renameField() {
        viewModel.renameField();
    }
}
