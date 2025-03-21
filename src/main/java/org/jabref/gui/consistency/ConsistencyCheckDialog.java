package org.jabref.gui.consistency;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.logic.quality.consistency.ConsistencyMessage;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.SpecialField;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

public class ConsistencyCheckDialog extends BaseDialog<Void> {

    @FXML private TableView<ConsistencyMessage> tableView;
    @FXML private ComboBox<String> entryTypeCombo;

    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;
    private final BibliographyConsistencyCheck.Result result;

    private ConsistencyCheckDialogViewModel viewModel;

    public ConsistencyCheckDialog(LibraryTab libraryTab,
                                  DialogService dialogService,
                                  GuiPreferences preferences,
                                  BibEntryTypesManager entryTypesManager,
                                  BibliographyConsistencyCheck.Result result) {
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;
        this.result = result;

        this.setTitle(Localization.lang("Check consistency"));
        this.initModality(Modality.NONE);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    private void onSelectionChanged(ListChangeListener.Change<? extends ConsistencyMessage> change) {
        if (change.next()) {
            change.getAddedSubList().stream().findFirst().ifPresent(message ->
                    libraryTab.showAndEdit(message.bibEntry()));
        }
    }

    public ConsistencyCheckDialogViewModel getViewModel() {
        return viewModel;
    }

    @FXML
    public void initialize() {
        viewModel = new ConsistencyCheckDialogViewModel(dialogService, preferences, entryTypesManager, result);

        tableView.getSelectionModel().getSelectedItems().addListener(this::onSelectionChanged);

        entryTypeCombo.getItems().addAll(viewModel.getEntryTypes());
        entryTypeCombo.valueProperty().bindBidirectional(viewModel.selectedEntryTypeProperty());
        EasyBind.listen(entryTypeCombo.getEditor().textProperty(), observable -> entryTypeCombo.commitValue());
        entryTypeCombo.getSelectionModel().selectFirst();

        FilteredList<ConsistencyMessage> filteredData = new FilteredList<>(viewModel.getTableData(), message ->
                message.message().getFirst().equals(viewModel.selectedEntryTypeProperty().get())
        );

        viewModel.selectedEntryTypeProperty().addListener((obs, oldValue, newValue) -> {
            filteredData.setPredicate(message ->
                    message.message().getFirst().equals(newValue)
            );
        });

        tableView.setItems(filteredData);

        int columnIndex = 0;
        for (String columnName : viewModel.getColumnNames()) {
            final int currentIndex = columnIndex;
            TableColumn<ConsistencyMessage, String> tableColumn = new TableColumn<>(columnName);

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
                        return;
                    }

                    ConsistencySymbol.fromText(item)
                                     .ifPresentOrElse(
                                             symbol -> setGraphic(symbol.getIcon().getGraphicNode()),
                                             () -> {
                                                 setGraphic(null);
                                                 setText(item);
                                             }
                                     );
                }
            });

            tableView.getColumns().add(tableColumn);
        }

        EnumSet<ConsistencySymbol> targetSymbols = EnumSet.of(
                ConsistencySymbol.OPTIONAL_FIELD_AT_ENTRY_TYPE_CELL_ENTRY,
                ConsistencySymbol.REQUIRED_FIELD_AT_ENTRY_TYPE_CELL_ENTRY,
                ConsistencySymbol.UNKNOWN_FIELD_AT_ENTRY_TYPE_CELL_ENTRY,
                ConsistencySymbol.UNSET_FIELD_AT_ENTRY_TYPE_CELL_ENTRY
        );

        targetSymbols.stream()
            .map(ConsistencySymbol::getText)
            .forEach(this::removeColumnWithUniformValue);

        Arrays.stream(SpecialField.values())
              .map(SpecialField::getDisplayName)
              .forEach(this::removeColumnByTitle);
    }

    private void removeColumnWithUniformValue(String symbol) {
        List<TableColumn<ConsistencyMessage, ?>> columnToRemove = tableView.getColumns().stream()
                                                                           .filter(column -> {
                                                                               Set<String> values = tableView.getItems().stream()
                                                                                                             .map(item -> Optional.ofNullable(column.getCellObservableValue(item).getValue()).map(Object::toString).orElse(""))
                                                                                                             .collect(Collectors.toSet());
                                                                               return values.size() == 1 && values.contains(symbol);
                                                                           })
                                                                           .toList();
        tableView.getColumns().removeAll(columnToRemove);
    }

    public void removeColumnByTitle(String columnName) {
        tableView.getColumns().removeIf(column -> column.getText().equalsIgnoreCase(columnName));
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
