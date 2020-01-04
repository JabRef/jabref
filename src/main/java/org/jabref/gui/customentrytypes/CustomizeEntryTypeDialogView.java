package org.jabref.gui.customentrytypes;

import java.util.EnumSet;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.customentrytypes.CustomEntryTypeDialogViewModel.FieldType;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.types.EntryType;

import com.airhacks.afterburner.views.ViewLoader;

public class CustomizeEntryTypeDialogView extends BaseDialog<Void> {

    @FXML private TableView<BibEntryType> entryTypes;
    @FXML private TableColumn<BibEntryType, String> entryTypColumn;
    @FXML private TableColumn<BibEntryType, String> entryTypeActionsColumn;
    @FXML private ComboBox<BibEntryType> addNewEntryType;
    @FXML private TableView<Field> requiredFields;
    @FXML private TableColumn<Field, String> requiredFieldsNameColumn;
    @FXML private TableColumn<Field, FieldType> fieldTypeColumn;
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

        entryTypColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getType().getDisplayName()));
        fieldTypeColumn.setCellFactory(cellData -> new RadioButtonCell<>(EnumSet.allOf(FieldType.class)));
        new ValueTableCellFactory<Field, FieldType>().withText(FieldType::getDisplayName).install(fieldTypeColumn);

        requiredFieldsNameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getDisplayName()));
        new ValueTableCellFactory<Field, FieldType>().withText(FieldType::getDisplayName).install(fieldTypeColumn);

        entryTypes.itemsProperty().bind(viewModel.entryTypesProperty());
        //TODO Change to the new viewmodel
        requiredFields.itemsProperty().bind(viewModel.fieldsProperty());
    }

    @FXML
    void addEntryType() {
        viewModel.addNewCustomEntryType();
    }

    @FXML
    void addNewField() {
        viewModel.addNewField();

    }

}
