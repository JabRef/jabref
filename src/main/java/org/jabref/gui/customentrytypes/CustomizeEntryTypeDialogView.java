package org.jabref.gui.customentrytypes;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.util.BaseDialog;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.types.EntryType;

import com.airhacks.afterburner.views.ViewLoader;

public class CustomizeEntryTypeDialogView extends BaseDialog<Void> {

    @FXML private TableView<EntryType> entryTypes;
    @FXML private TableColumn<EntryType, String> entryTypColumn;
    @FXML private TableColumn<EntryType, String> entryTypeActionsColumn;
    @FXML private ComboBox<EntryType> addNewEntryType;
    @FXML private TableView<Field> requiredFields;
    @FXML private TableColumn<Field, String> requiredFieldsNameColumn;
    @FXML private TableColumn<Field, String> fieldTypeColumn;
    @FXML private TableColumn<Field, String> fieldTypeActionColumn;
    @FXML private ComboBox<?> addNewField;
    @FXML private ButtonType applyButton;

    private final CustomEntryTypeDialogViewModel viewModel;

    public CustomizeEntryTypeDialogView(BibDatabaseContext bibDatabaseContext) {

        viewModel = new CustomEntryTypeDialogViewModel();

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        setupTable();
    }

    private void setupTable() {
        entryTypColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getDisplayName()));

    }

    @FXML
    void addEntryType() {
        viewModel.addNewCustomEntryType();
    }

    @FXML
    void addRequiredFields() {
        viewModel.addNewRequiredField();

    }

    @FXML
    void addOptionalField() {
        viewModel.addNewOptionalField();

    }

    @FXML
    void addOptionalField2() {
        viewModel.addNewOptionalField2();

    }

}
