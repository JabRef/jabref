package org.jabref.gui.customentrytypes;

import java.util.EnumSet;

import javax.inject.Inject;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

import org.jabref.gui.DialogService;
import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.StateManager;
import org.jabref.gui.customentrytypes.CustomEntryTypeDialogViewModel.FieldType;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.RadioButtonCell;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class CustomizeEntryTypeDialogView extends BaseDialog<Void> {

    private final BibDatabaseMode mode;
    private final BibEntryTypesManager entryTypesManager;

    @FXML private TableView<EntryTypeViewModel> entryTypes;
    @FXML private TableColumn<EntryTypeViewModel, String> entryTypColumn;
    @FXML private TableColumn<EntryTypeViewModel, String> entryTypeActionsColumn;
    @FXML private TextField addNewEntryType;
    @FXML private TableView<FieldViewModel> fields;
    @FXML private TableColumn<FieldViewModel, String> fieldNameColumn;
    @FXML private TableColumn<FieldViewModel, FieldType> fieldTypeColumn;
    @FXML private TableColumn<FieldViewModel, String> fieldTypeActionColumn;
    @FXML private ComboBox<Field> addNewField;
    @FXML private ButtonType applyButton;
    @FXML private ButtonType resetButton;
    @FXML private Button addNewEntryTypeButton;
    @FXML private Button addNewFieldButton;

    @Inject private PreferencesService preferencesService;
    @Inject private StateManager stateManager;
    @Inject private DialogService dialogService;

    private CustomEntryTypeDialogViewModel viewModel;
    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();
    private CustomLocalDragboard localDragboard;

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
        ControlHelper.setAction(resetButton, getDialogPane(), event -> this.resetEntryTypes());
    }

    @FXML
    private void initialize() {
        // As the state manager gets injected it's not available in the constructor
        this.localDragboard = stateManager.getLocalDragboard();

        viewModel = new CustomEntryTypeDialogViewModel(mode, preferencesService, entryTypesManager);
        setupTable();

        addNewEntryTypeButton.disableProperty().bind(viewModel.entryTypeValidationStatus().validProperty().not());
        addNewFieldButton.disableProperty().bind(viewModel.fieldValidationStatus().validProperty().not());

        Platform.runLater(() -> {
            visualizer.initVisualization(viewModel.entryTypeValidationStatus(), addNewEntryType, true);
            visualizer.initVisualization(viewModel.fieldValidationStatus(), addNewField, true);
        });
    }

    private void setupTable() {

        // Table View must be editable, otherwise the change of the Radiobuttons does not propagate the commit event
        fields.setEditable(true);
        entryTypColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().entryType().get().getType().getDisplayName()));
        entryTypes.setItems(viewModel.entryTypes());
        entryTypes.getSelectionModel().selectFirst();

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
                        return (Localization.lang("Remove entry type") + " " + name);
                    } else {
                        return null;
                    }
                })
                .withOnMouseClickedEvent((type, name) -> {
                    if (type instanceof CustomEntryTypeViewModel) {
                        return evt -> viewModel.removeEntryType(entryTypes.getSelectionModel().getSelectedItem());
                    } else {
                        return evt -> {
                        };
                    }
                })
                .install(entryTypeActionsColumn);

        fieldTypeColumn.setCellFactory(cellData -> new RadioButtonCell<>(EnumSet.allOf(FieldType.class)));
        fieldTypeColumn.setCellValueFactory(item -> item.getValue().fieldType());

        fieldTypeColumn.setEditable(true);
        fieldTypeColumn.setOnEditCommit(event -> {
            event.getTableView().getItems().get(event.getTablePosition().getRow()).setFieldType(event.getNewValue());
        });

        fieldNameColumn.setCellValueFactory(item -> item.getValue().fieldName());

        viewModel.selectedEntryTypeProperty().bind(entryTypes.getSelectionModel().selectedItemProperty());
        viewModel.entryTypeToAddProperty().bindBidirectional(addNewEntryType.textProperty());

        addNewField.setItems(viewModel.fieldsForAdding());
        addNewField.setConverter(viewModel.FIELD_STRING_CONVERTER);

        fieldTypeActionColumn.setSortable(false);
        fieldTypeActionColumn.setReorderable(false);
        fieldTypeActionColumn.setEditable(false);
        fieldTypeActionColumn.setCellValueFactory(cellData -> cellData.getValue().fieldName());

        new ValueTableCellFactory<FieldViewModel, String>()
                .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove field %0 from currently selected entry type", name))
                .withOnMouseClickedEvent(item -> evt -> {
                    viewModel.removeField(fields.getSelectionModel().getSelectedItem());
                })
                .install(fieldTypeActionColumn);

        viewModel.newFieldToAddProperty().bindBidirectional(addNewField.valueProperty());
        // The valueProperty() of addNewField ComboBox needs to be updated by typing text in the ComboBox textfield,
        // since the enabled/disabled state of addNewFieldButton won't update otherwise
        EasyBind.subscribe(addNewField.getEditor().textProperty(), text -> addNewField.setValue(CustomEntryTypeDialogViewModel.FIELD_STRING_CONVERTER.fromString(text)));

        EasyBind.subscribe(viewModel.selectedEntryTypeProperty(), type -> {
            if (type != null) {
                var items = type.fields();
                fields.setItems(items);
            }
        });

        new ViewModelTableRowFactory<FieldViewModel>()
              .setOnDragDetected(this::handleOnDragDetected)
              .setOnDragDropped(this::handleOnDragDropped)
              .setOnDragOver(this::handleOnDragOver)
              .setOnDragExited(this::handleOnDragExited)
              .install(fields);
}

    private void handleOnDragOver(TableRow<FieldViewModel> row, FieldViewModel originalItem, DragEvent event) {
        if ((event.getGestureSource() != originalItem) && event.getDragboard().hasContent(DragAndDropDataFormats.FIELD)) {
            event.acceptTransferModes(TransferMode.MOVE);
            ControlHelper.setDroppingPseudoClasses(row, event);
        }
    }

    private void handleOnDragDetected(TableRow<FieldViewModel> row, FieldViewModel fieldViewModel, MouseEvent event) {
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
        this.entryTypes.getSelectionModel().select(newlyAdded);
        this.entryTypes.scrollTo(newlyAdded);
    }

    @FXML
    void addNewField() {
        viewModel.addNewField();
    }

    private void resetEntryTypes() {
        boolean reset = dialogService.showConfirmationDialogAndWait(
                                            Localization.lang("Reset entry types and fields to defaults"),
                                            Localization.lang("This will reset all entry types to their default values and remove all custom entry types"),
                                            Localization.lang("Reset to default"));
        if (reset) {
            viewModel.resetAllCustomEntryTypes();
            viewModel.addAllTypes();
            this.entryTypes.refresh();
        }

    }
}
