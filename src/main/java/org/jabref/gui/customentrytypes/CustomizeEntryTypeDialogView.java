package org.jabref.gui.customentrytypes;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;
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

    @FXML private TableColumn<Field, String> requiredFieldsActionColumn;
    @FXML private ComboBox<Field> addNewRequiredField;
    @FXML private TableView<Field> optionalFields;
    @FXML private TableColumn<Field, String> optionalFieldNameColumn;
    @FXML private TableColumn<Field, String> optionalFieldActionColumn;

    @FXML private ComboBox<Field> addOptionalField;
    @FXML private TableView<Field> optionalFields2;
    @FXML private TableColumn<Field, String> optionalFields2NameColumn;
    @FXML private TableColumn<Field, String> optionalFields2ActionColumn;

    @FXML private ComboBox<Field> addOptionalFields2;
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
        requiredFieldsNameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getDisplayName()));
        optionalFieldNameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getDisplayName()));
        optionalFields2NameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getDisplayName()));

        new ValueTableCellFactory<EntryType, String>()
        .withText(name -> name)
        .install(entryTypColumn);

        new ValueTableCellFactory<EntryType, String>()
        .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
        .withTooltip(name -> Localization.lang("Remove entry type") + " " + name)
        .withOnMouseClickedEvent(item -> evt ->
                viewModel.removeEntryType(entryTypes.getFocusModel().getFocusedItem()))
        .install(entryTypeActionsColumn);

        new ValueTableCellFactory<Field, String>()
        .withText(name -> name)
        .install(requiredFieldsNameColumn);

        new ValueTableCellFactory<Field, String>()
        .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
        .withTooltip(name -> Localization.lang("Remove required field") + " " + name)
        .withOnMouseClickedEvent(item -> evt ->
                viewModel.removeRequiredField(requiredFields.getFocusModel().getFocusedItem()))
        .install(requiredFieldsActionColumn);

        new ValueTableCellFactory<Field, String>()
        .withText(name -> name)
        .install(optionalFieldNameColumn);

        new ValueTableCellFactory<Field, String>()
        .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
        .withTooltip(name -> Localization.lang("Remove optional field") + " " + name)
        .withOnMouseClickedEvent(item -> evt ->
                viewModel.removeOptionalField(optionalFields.getFocusModel().getFocusedItem()))
        .install(optionalFieldActionColumn);

        new ValueTableCellFactory<Field, String>()
        .withText(name -> name)
        .install(optionalFields2NameColumn);

        new ValueTableCellFactory<Field, String>()
        .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
        .withTooltip(name -> Localization.lang("Remove optional2 field") + " " + name)
        .withOnMouseClickedEvent(item -> evt ->
                viewModel.removeOptional2Field(optionalFields2.getFocusModel().getFocusedItem()))
        .install(optionalFields2ActionColumn);
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
