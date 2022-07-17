package org.jabref.gui.edit.automaticfiededitor.copyormovecontent;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
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

public class CopyOrMoveFieldContentTabView extends AbstractAutomaticFieldEditorTabView implements AutomaticFieldEditorTab {
    @FXML
    private Button moveContentButton;

    @FXML
    private Button swapContentButton;

    @FXML
    private ComboBox<Field> fromFieldComboBox;
    @FXML
    private ComboBox<Field> toFieldComboBox;

    @FXML
    private CheckBox overwriteFieldContentCheckBox;

    private CopyOrMoveFieldContentTabViewModel viewModel;
    private final List<BibEntry> selectedEntries;
    private final BibDatabase database;
    private final NamedCompound dialogEdits;

    public CopyOrMoveFieldContentTabView(List<BibEntry> selectedEntries, BibDatabase database, NamedCompound dialogEdits) {
        this.selectedEntries = new ArrayList<>(selectedEntries);
        this.database = database;
        this.dialogEdits = dialogEdits;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        viewModel = new CopyOrMoveFieldContentTabViewModel(selectedEntries, database, dialogEdits);
        initializeFromAndToComboBox();

        viewModel.overwriteFieldContentProperty().bindBidirectional(overwriteFieldContentCheckBox.selectedProperty());

        moveContentButton.disableProperty().bind(viewModel.overwriteFieldContentProperty().not());
        swapContentButton.disableProperty().bind(viewModel.overwriteFieldContentProperty().not());
    }

    private void initializeFromAndToComboBox() {
        fromFieldComboBox.getItems().setAll(viewModel.getAllFields());
        toFieldComboBox.getItems().setAll(viewModel.getAllFields());

        fromFieldComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Field field) {
                return field.getDisplayName();
            }

            @Override
            public Field fromString(String name) {
                return FieldFactory.parseField(name);
            }
        });

        toFieldComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Field field) {
                return field == null ? "" : field.getDisplayName();
            }

            @Override
            public Field fromString(String name) {
                return FieldFactory.parseField(name);
            }
        });

        fromFieldComboBox.valueProperty().bindBidirectional(viewModel.fromFieldProperty());
        toFieldComboBox.valueProperty().bindBidirectional(viewModel.toFieldProperty());
    }

    @Override
    public String getTabName() {
        return Localization.lang("Copy or Move content");
    }

    @FXML
    void copyContent() {
        viewModel.copyValue();
    }

    @FXML
    void moveContent() {
        viewModel.moveValue();
    }

    @FXML
    void swapContent() {
        viewModel.swapValues();
    }
}
