package org.jabref.gui.customentrytypes;

import java.util.EnumSet;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import org.jabref.gui.customentrytypes.CustomEntryTypeDialogViewModel.FieldType;
import org.jabref.gui.util.BaseDialog;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;

public class CustomizeEntryTypeDialogView extends BaseDialog<Void> {

    @FXML private TableView<BibEntryType> entryTypes;
    @FXML private TableColumn<BibEntryType, String> entryTypColumn;
    @FXML private TableColumn<BibEntryType, String> entryTypeActionsColumn;
    @FXML private TextField addNewEntryType;
    @FXML private TableView<FieldViewModel> requiredFields;
    @FXML private TableColumn<FieldViewModel, String> fieldNameColumn;
    @FXML private TableColumn<FieldViewModel, FieldType> fieldTypeColumn;
    @FXML private TableColumn<FieldViewModel, String> fieldTypeActionColumn;
    @FXML private ComboBox<Field> addNewField;
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
        entryTypes.setItems(viewModel.entryTypesProperty());
        entryTypes.getSelectionModel().selectFirst();

        fieldTypeColumn.setCellFactory(cellData -> new RadioButtonCell<>(EnumSet.allOf(FieldType.class)));
        fieldTypeColumn.setCellValueFactory(item -> item.getValue().fieldTypeProperty());

        fieldNameColumn.setCellValueFactory(item -> item.getValue().fieldNameProperty());

        viewModel.selectedEntryTypeProperty().bind(entryTypes.getSelectionModel().selectedItemProperty());

        viewModel.entryTypeToAddProperty().bind(addNewEntryType.textProperty());

        addNewField.setItems(viewModel.fieldsProperty());
        requiredFields.itemsProperty().bindBidirectional(viewModel.fieldsforTypesProperty());

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
