package org.jabref.gui.customentrytypes;

import java.util.EnumSet;

import javax.inject.Inject;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import org.jabref.gui.customentrytypes.CustomEntryTypeDialogViewModel.FieldType;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.RadioButtonCell;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class CustomizeEntryTypeDialogView extends BaseDialog<Void> {

    private final BibDatabaseMode mode;
    private final BibEntryTypesManager entryTypesManager;

    @FXML private TableView<BibEntryType> entryTypes;
    @FXML private TableColumn<BibEntryType, String> entryTypColumn;
    @FXML private TableColumn<BibEntryType, String> entryTypeActionsColumn;
    @FXML private TextField addNewEntryType;
    @FXML private TableView<FieldViewModel> fields;
    @FXML private TableColumn<FieldViewModel, String> fieldNameColumn;
    @FXML private TableColumn<FieldViewModel, FieldType> fieldTypeColumn;
    @FXML private TableColumn<FieldViewModel, String> fieldTypeActionColumn;
    @FXML private ComboBox<Field> addNewField;
    @FXML private ButtonType applyButton;

    @Inject private PreferencesService preferencesService;

    private CustomEntryTypeDialogViewModel viewModel;

    public CustomizeEntryTypeDialogView(BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager) {
        this.mode = bibDatabaseContext.getMode();
        this.entryTypesManager = entryTypesManager;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(button -> {
            if (button.getButtonData() == ButtonData.OK_DONE) {
                viewModel.apply();
            }
            return null;
        });
    }

    @FXML
    private void initialize() {
        viewModel = new CustomEntryTypeDialogViewModel(mode, preferencesService, entryTypesManager);
        setupTable();
    }

    private void setupTable() {

        fields.setEditable(true); //Table View must be editable, otherwise the change of the Radiobuttons does not propagate the commit event
        entryTypColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getType().getDisplayName()));
        entryTypes.itemsProperty().bind(viewModel.entryTypes());
        entryTypes.getSelectionModel().selectFirst();

        entryTypeActionsColumn.setSortable(false);
        entryTypeActionsColumn.setReorderable(false);
        entryTypeActionsColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getType().getDisplayName()));
        new ValueTableCellFactory<BibEntryType, String>()
             .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
             .withTooltip(name -> Localization.lang("Remove entry type") + " " + name)
             .withOnMouseClickedEvent(item -> evt -> viewModel.removeEntryType(entryTypes.getSelectionModel().getSelectedItem()))
             .install(entryTypeActionsColumn);

        fieldTypeColumn.setCellFactory(cellData -> new RadioButtonCell<>(EnumSet.allOf(FieldType.class)));
        fieldTypeColumn.setCellValueFactory(item -> item.getValue().fieldType());

        fieldTypeColumn.setEditable(true);
        fieldTypeColumn.setOnEditCommit(event -> {
            event.getTableView().getItems().get(event.getTablePosition().getRow()).setFieldType(event.getNewValue());
        });

        fieldNameColumn.setCellValueFactory(item -> item.getValue().fieldName());

        viewModel.selectedEntryTypeProperty().bind(entryTypes.getSelectionModel().selectedItemProperty());
        viewModel.entryTypeToAddProperty().bind(addNewEntryType.textProperty());

        addNewField.setItems(viewModel.fields());
        addNewField.setConverter(viewModel.FIELD_STRING_CONVERTER);

        fieldTypeActionColumn.setSortable(false);
        fieldTypeActionColumn.setReorderable(false);
        fieldTypeActionColumn.setCellValueFactory(cellData -> cellData.getValue().fieldName());

        new ValueTableCellFactory<FieldViewModel, String>()
           .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
           .withTooltip(name -> Localization.lang("Remove field from entry type") + " " + name)
           .withOnMouseClickedEvent(item -> evt -> viewModel.removeField(fields.getSelectionModel().getSelectedItem()))
           .install(fieldTypeActionColumn);

        viewModel.newFieldToAddProperty().bind(addNewField.valueProperty());
        fields.itemsProperty().bindBidirectional(viewModel.fieldsforTypesProperty());
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
