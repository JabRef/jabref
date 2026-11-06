package org.jabref.gui.consistency;

import java.util.Arrays;
import java.util.List;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.stage.Modality;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.ConsistencyMessage;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.entry.field.SpecialField;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import jakarta.inject.Inject;

public class ConsistencyCheckDialog extends BaseDialog<Void> {

    @FXML private TableView<ConsistencyMessage> tableView;
    @FXML private ComboBox<String> entryTypeCombo;
    @FXML private RadioButton restrictToBstFieldsRadio;
    @FXML private Label bstStyleLabel;
    @FXML private RadioButton restrictToAuxEntriesRadio;
    @FXML private Label auxFileLabel;

    @Inject private StateManager stateManager;
    @Inject private EntryEditor entryEditor;

    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final ConsistencyCheckDialogViewModel viewModel;

    public ConsistencyCheckDialog(LibraryTab libraryTab,
                                  DialogService dialogService,
                                  ConsistencyCheckDialogViewModel viewModel) {
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.viewModel = viewModel;

        this.setTitle(Localization.lang("Check consistency"));
        this.initModality(Modality.NONE);
        this.setOnHidden(_ -> viewModel.cancelPendingLoads());

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    public ConsistencyCheckDialogViewModel getViewModel() {
        return viewModel;
    }

    @FXML
    public void initialize() {
        entryTypeCombo.setItems(viewModel.getEntryTypes());
        entryTypeCombo.valueProperty().bindBidirectional(viewModel.selectedEntryTypeProperty());
        EasyBind.listen(entryTypeCombo.getEditor().textProperty(), _ -> entryTypeCombo.commitValue());
        entryTypeCombo.getSelectionModel().selectFirst();

        FilteredList<ConsistencyMessage> filteredData = new FilteredList<>(viewModel.getTableData(), message ->
                message.message().getFirst().equals(viewModel.selectedEntryTypeProperty().get())
        );

        viewModel.selectedEntryTypeProperty().addListener((_, _, newValue) ->
                filteredData.setPredicate(message ->
                        message.message().getFirst().equals(newValue)
                ));

        tableView.setItems(filteredData);

        restrictToBstFieldsRadio.selectedProperty().bindBidirectional(viewModel.restrictToBstFieldsProperty());
        restrictToBstFieldsRadio.disableProperty().bind(viewModel.bstStyleNameProperty().isEmpty());
        bstStyleLabel.textProperty().bind(viewModel.bstStyleNameProperty());

        restrictToAuxEntriesRadio.selectedProperty().bindBidirectional(viewModel.restrictToAuxEntriesProperty());
        restrictToAuxEntriesRadio.disableProperty().bind(viewModel.auxFileNameProperty().isEmpty());
        auxFileLabel.textProperty().bind(viewModel.auxFileNameProperty());
        tableView.setPlaceholder(new Label(Localization.lang("No problems found.")));
        rebuildColumns();
        viewModel.reportRevisionProperty().addListener(_ -> rebuildColumns());

        // A hidden column must not keep sorting the rows
        EasyBind.listen(viewModel.shownFieldsProperty(), _ -> tableView.getSortOrder().removeIf(column -> !column.isVisible()));
    }

    private void rebuildColumns() {
        tableView.getSortOrder().clear();
        tableView.getColumns().forEach(column -> column.visibleProperty().unbind());
        tableView.getColumns().clear();
        List<Field> reportedFields = viewModel.getReportedFields();
        int columnIndex = 0;
        // [impl->req~ux.consistency.missing-field-columns~1]
        for (String columnName : viewModel.getColumnNames()) {
            final int currentIndex = columnIndex;
            TableColumn<ConsistencyMessage, String> tableColumn = new TableColumn<>(columnName);

            // The first two columns (entry type and citation key) are always shown
            if (currentIndex >= 2) {
                Field field = reportedFields.get(currentIndex - 2);
                // [impl->req~ux.consistency.bst-field-columns~2]
                tableColumn.visibleProperty().bind(viewModel.shownFieldsProperty().map(shown -> shown.test(field)));
            }

            tableColumn.setCellValueFactory(row -> {
                List<String> message = row.getValue().message();
                if (currentIndex < message.size()) {
                    return new ReadOnlyStringWrapper(message.get(currentIndex));
                }
                return new ReadOnlyStringWrapper("");
            });
            columnIndex++;

            tableColumn.setCellFactory(_ -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setGraphic(null);
                        setText(null);
                        setTooltip(null);
                        return;
                    }

                    ConsistencySymbol.fromText(item)
                                     .ifPresentOrElse(
                                             symbol -> {
                                                 setText(null);
                                                 setGraphic(symbol.getIcon().getGraphicNode());
                                                 setTooltip(new Tooltip(symbol.getDescription()));
                                             },
                                             () -> {
                                                 setGraphic(null);
                                                 setTooltip(null);
                                                 setText(item);
                                             }
                                     );

                    this.setOnMouseClicked(_ -> {
                        if (!isEmpty()) {
                            TableColumn<ConsistencyMessage, String> clickedColumn = getTableColumn();

                            ConsistencyMessage message = getTableRow().getItem();
                            String cellValue = getTableColumn().getCellObservableValue(getIndex()).getValue();
                            Field field = FieldFactory.parseField(clickedColumn.getText());
                            boolean isUnsetField = cellValue.equals(ConsistencySymbol.UNSET_FIELD_AT_ENTRY_TYPE_CELL_ENTRY.getText());

                            if (!field.isStandardField() && !message.bibEntry().hasField(field) && isUnsetField) {
                                libraryTab.showAndEdit(message.bibEntry());
                            } else {
                                stateManager.getEditorShowing().setValue(true);
                                libraryTab.clearAndSelect(message.bibEntry());
                                entryEditor.setFocusToField(field);
                            }
                        }
                    });
                }
            });

            tableView.getColumns().add(tableColumn);
        }

        Arrays.stream(SpecialField.values())
              .map(FieldTextMapper::getDisplayName)
              .forEach(this::removeColumnByTitle);
    }

    public void removeColumnByTitle(String columnName) {
        tableView.getColumns().removeIf(column -> column.getText().equalsIgnoreCase(columnName));
    }

    @FXML
    private void browseBstFile() {
        viewModel.browseBstFile();
    }

    @FXML
    private void browseAuxFile() {
        viewModel.browseAuxFile();
    }

    @FXML
    private void exportAsCsv() {
        viewModel.startExportAsCsv();
    }

    @FXML
    private void exportAsTxt() {
        viewModel.startExportAsTxt();
    }

    @FXML
    private void showInfo() {
        ConsistencySymbolsDialog consistencySymbolsDialog = new ConsistencySymbolsDialog();
        dialogService.showCustomDialog(consistencySymbolsDialog);
    }
}
