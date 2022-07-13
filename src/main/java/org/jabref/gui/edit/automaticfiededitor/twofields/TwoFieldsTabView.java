package org.jabref.gui.edit.automaticfiededitor.twofields;

import java.util.ArrayList;
import java.util.Comparator;
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
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import com.airhacks.afterburner.views.ViewLoader;

public class TwoFieldsTabView extends AbstractAutomaticFieldEditorTabView implements AutomaticFieldEditorTab {
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

    private TwoFieldsViewModel viewModel;
    private final List<BibEntry> selectedEntries;
    private final BibDatabaseContext databaseContext;
    private final NamedCompound dialogEdits;

    public TwoFieldsTabView(List<BibEntry> selectedEntries, BibDatabaseContext databaseContext, NamedCompound dialogEdits) {
        this.selectedEntries = new ArrayList<>(selectedEntries);
        this.databaseContext = databaseContext;
        this.dialogEdits = dialogEdits;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        viewModel = new TwoFieldsViewModel(selectedEntries, databaseContext.getDatabase().getAllVisibleFields(), dialogEdits);
        initializeFromAndToComboBox();

        viewModel.overwriteFieldContentProperty().bindBidirectional(overwriteFieldContentCheckBox.selectedProperty());

        moveContentButton.disableProperty().bind(viewModel.overwriteFieldContentProperty().not());
        swapContentButton.disableProperty().bind(viewModel.overwriteFieldContentProperty().not());
    }

    private void initializeFromAndToComboBox() {
        fromFieldComboBox.getItems().addAll(viewModel.getAllFields().sorted(Comparator.comparing(Field::getName)));
        toFieldComboBox.getItems().addAll(viewModel.getAllFields().sorted(Comparator.comparing(Field::getName)));

        fromFieldComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Field field) {
                return field.getName();
            }

            @Override
            public Field fromString(String name) {
                return FieldFactory.parseField(name);
            }
        });

        toFieldComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Field field) {
                return field.getName();
            }

            @Override
            public Field fromString(String name) {
                return FieldFactory.parseField(name);
            }
        });

        fromFieldComboBox.getSelectionModel().selectFirst();
        toFieldComboBox.getSelectionModel().selectLast();

        viewModel.fromFieldProperty().bindBidirectional(fromFieldComboBox.valueProperty());
        viewModel.toFieldProperty().bindBidirectional(toFieldComboBox.valueProperty());
    }

    @Override
    public String getTabName() {
        return Localization.lang("Two fields");
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
