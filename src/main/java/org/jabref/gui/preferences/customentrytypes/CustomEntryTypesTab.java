package org.jabref.gui.preferences.customentrytypes;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.FieldsUtil;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import jakarta.inject.Inject;

public class CustomEntryTypesTab extends AbstractPreferenceTabView<CustomEntryTypesTabViewModel> implements PreferencesTab {

    @FXML private TableView<EntryTypeViewModel> entryTypesTable;
    @FXML private TableColumn<EntryTypeViewModel, String> entryTypColumn;
    @FXML private TableColumn<EntryTypeViewModel, String> entryTypeActionsColumn;
    @FXML private TextField addNewEntryType;
    @FXML private TableView<FieldViewModel> fields;
    @FXML private TableColumn<FieldViewModel, String> fieldNameColumn;
    @FXML private TableColumn<FieldViewModel, Boolean> fieldTypeColumn;
    @FXML private TableColumn<FieldViewModel, String> fieldTypeActionColumn;
    @FXML private TableColumn<FieldViewModel, Boolean> fieldTypeMultilineColumn;
    @FXML private ComboBox<Field> addNewField;
    @FXML private Button addNewEntryTypeButton;
    @FXML private Button addNewFieldButton;

    @Inject private StateManager stateManager;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    private CustomLocalDragboard localDragboard;

    public CustomEntryTypesTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry types");
    }

    public void initialize() {
        BibDatabaseMode mode = stateManager.getActiveDatabase().map(BibDatabaseContext::getMode)
                                           .orElse(preferencesService.getLibraryPreferences().getDefaultBibDatabaseMode());
        BibEntryTypesManager entryTypesRepository = preferencesService.getCustomEntryTypesRepository();

        this.viewModel = new CustomEntryTypesTabViewModel(mode, entryTypesRepository, dialogService, preferencesService);

        // As the state manager gets injected it's not available in the constructor
        this.localDragboard = stateManager.getLocalDragboard();

        setupEntryTypesTable();
        setupFieldsTable();

        addNewEntryTypeButton.disableProperty().bind(viewModel.entryTypeValidationStatus().validProperty().not());
        addNewFieldButton.disableProperty().bind(viewModel.fieldValidationStatus().validProperty().not());

        Platform.runLater(() -> {
            visualizer.initVisualization(viewModel.entryTypeValidationStatus(), addNewEntryType, true);
            visualizer.initVisualization(viewModel.fieldValidationStatus(), addNewField, true);
        });
    }

    private void setupEntryTypesTable() {
        // Table View must be editable, otherwise the change of the Radiobuttons does not propagate the commit event
        fields.setEditable(true);
        entryTypColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().entryType().get().getType().getDisplayName()));
        entryTypesTable.setItems(viewModel.entryTypes());
        entryTypesTable.getSelectionModel().selectFirst();

        entryTypeActionsColumn.setSortable(false);
        entryTypeActionsColumn.setReorderable(false);
        entryTypeActionsColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().entryType().get().getType().getDisplayName()));
        new ValueTableCellFactory<EntryTypeViewModel, String>()
                .withGraphic((type, name) -> {
                    if (type instanceof CustomEntryTypeViewModel) {
                        return IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode();
                    } else {
                        return null;
                    }
                })
                .withTooltip((type, name) -> {
                    if (type instanceof CustomEntryTypeViewModel) {
                        return Localization.lang("Remove entry type") + " " + name;
                    } else {
                        return null;
                    }
                })
                .withOnMouseClickedEvent((type, name) -> {
                    if (type instanceof CustomEntryTypeViewModel) {
                        return evt -> viewModel.removeEntryType(entryTypesTable.getSelectionModel().getSelectedItem());
                    } else {
                        return evt -> {
                        };
                    }
                })
                .install(entryTypeActionsColumn);

        viewModel.selectedEntryTypeProperty().bind(entryTypesTable.getSelectionModel().selectedItemProperty());
        viewModel.entryTypeToAddProperty().bindBidirectional(addNewEntryType.textProperty());

        EasyBind.subscribe(viewModel.selectedEntryTypeProperty(), type -> {
            if (type != null) {
                var items = type.fields();
                fields.setItems(items);
            } else {
                fields.setItems(null);
            }
        });
    }

    private void setupFieldsTable() {
        fieldNameColumn.setCellValueFactory(item -> item.getValue().displayNameProperty());
        fieldNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        fieldNameColumn.setEditable(true);
        fieldNameColumn.setOnEditCommit((TableColumn.CellEditEvent<FieldViewModel, String> event) -> {
            String newDisplayName = event.getNewValue();
            if (newDisplayName.isBlank()) {
                dialogService.notify(Localization.lang("Name cannot be empty"));
                event.getTableView().edit(-1, null);
                event.getTableView().refresh();
                return;
            }

            FieldViewModel fieldViewModel = event.getRowValue();
            String currentDisplayName = fieldViewModel.displayNameProperty().getValue();
            EntryTypeViewModel selectedEntryType = viewModel.selectedEntryTypeProperty().get();
            ObservableList<FieldViewModel> entryFields = selectedEntryType.fields();
            // The first predicate will check if the user input the original field name or doesn't edit anything after double click
            boolean fieldExists = !newDisplayName.equals(currentDisplayName) && viewModel.displayNameExists(newDisplayName);
            if (fieldExists) {
                dialogService.notify(Localization.lang("Unable to change field name. \"%0\" already in use.", newDisplayName));
                event.getTableView().edit(-1, null);
            } else {
                fieldViewModel.displayNameProperty().setValue(newDisplayName);
            }
            event.getTableView().refresh();
        });

        fieldTypeColumn.setCellFactory(CheckBoxTableCell.forTableColumn(fieldTypeColumn));
        fieldTypeColumn.setCellValueFactory(item -> item.getValue().requiredProperty());
        makeRotatedColumnHeader(fieldTypeColumn, Localization.lang("Required"));

        fieldTypeMultilineColumn.setCellFactory(CheckBoxTableCell.forTableColumn(fieldTypeMultilineColumn));
        fieldTypeMultilineColumn.setCellValueFactory(item -> item.getValue().multilineProperty());
        makeRotatedColumnHeader(fieldTypeMultilineColumn, Localization.lang("Multiline"));

        fieldTypeActionColumn.setSortable(false);
        fieldTypeActionColumn.setReorderable(false);
        fieldTypeActionColumn.setEditable(false);
        fieldTypeActionColumn.setCellValueFactory(cellData -> cellData.getValue().displayNameProperty());

        new ValueTableCellFactory<FieldViewModel, String>()
                .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove field %0 from currently selected entry type", name))
                .withOnMouseClickedEvent(item -> evt -> viewModel.removeField(fields.getSelectionModel().getSelectedItem()))
                .install(fieldTypeActionColumn);

        new ViewModelTableRowFactory<FieldViewModel>()
                .setOnDragDetected(this::handleOnDragDetected)
                .setOnDragDropped(this::handleOnDragDropped)
                .setOnDragOver(this::handleOnDragOver)
                .setOnDragExited(this::handleOnDragExited)
                .install(fields);

        addNewField.setItems(viewModel.fieldsForAdding());
        addNewField.setConverter(FieldsUtil.FIELD_STRING_CONVERTER);

        viewModel.newFieldToAddProperty().bindBidirectional(addNewField.valueProperty());
        // The valueProperty() of addNewField ComboBox needs to be updated by typing text in the ComboBox textfield,
        // since the enabled/disabled state of addNewFieldButton won't update otherwise
        EasyBind.subscribe(addNewField.getEditor().textProperty(), text -> addNewField.setValue(FieldsUtil.FIELD_STRING_CONVERTER.fromString(text)));
    }

    private void makeRotatedColumnHeader(TableColumn<?, ?> column, String text) {
        Label label = new Label();
        label.setText(text);
        label.setRotate(-90);
        label.setMinWidth(80);
        column.setGraphic(new Group(label));
        column.getStyleClass().add("rotated");
    }

    private void handleOnDragOver(TableRow<FieldViewModel> row, FieldViewModel originalItem, DragEvent
            event) {
        if ((event.getGestureSource() != originalItem) && event.getDragboard().hasContent(DragAndDropDataFormats.FIELD)) {
            event.acceptTransferModes(TransferMode.MOVE);
            ControlHelper.setDroppingPseudoClasses(row, event);
        }
    }

    private void handleOnDragDetected(TableRow<FieldViewModel> row, FieldViewModel fieldViewModel, MouseEvent
            event) {
        row.startFullDrag();
        FieldViewModel field = fields.getSelectionModel().getSelectedItem();

        ClipboardContent content = new ClipboardContent();
        Dragboard dragboard = fields.startDragAndDrop(TransferMode.MOVE);
        content.put(DragAndDropDataFormats.FIELD, "");
        localDragboard.putValue(FieldViewModel.class, field);
        dragboard.setContent(content);
        event.consume();
    }

    private void handleOnDragDropped(TableRow<FieldViewModel> row, FieldViewModel originalItem, DragEvent event) {
        if (localDragboard.hasType(FieldViewModel.class)) {
            FieldViewModel field = localDragboard.getValue(FieldViewModel.class);
            fields.getItems().remove(field);

            if (row.isEmpty()) {
                fields.getItems().add(field);
            } else {
                // decide based on drop position whether to add the element before or after
                int offset = event.getY() > (row.getHeight() / 2) ? 1 : 0;
                fields.getItems().add(row.getIndex() + offset, field);
            }
        }
        event.setDropCompleted(true);
        event.consume();
    }

    private void handleOnDragExited(TableRow<FieldViewModel> row, FieldViewModel fieldViewModel, DragEvent dragEvent) {
        ControlHelper.removeDroppingPseudoClasses(row);
    }

    @FXML
    void addEntryType() {
        EntryTypeViewModel newlyAdded = viewModel.addNewCustomEntryType();
        this.entryTypesTable.getSelectionModel().select(newlyAdded);
        this.entryTypesTable.scrollTo(newlyAdded);
    }

    @FXML
    void addNewField() {
        viewModel.addNewField();
    }

    @FXML
    void resetEntryTypes() {
        boolean reset = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Reset entry types and fields to defaults"),
                Localization.lang("This will reset all entry types to their default values and remove all custom entry types"),
                Localization.lang("Reset to default"));
        if (reset) {
            viewModel.resetAllCustomEntryTypes();
            fields.getSelectionModel().clearSelection();
            entryTypesTable.getSelectionModel().clearSelection();
            viewModel.setValues();
            entryTypesTable.refresh();
        }
    }
}
