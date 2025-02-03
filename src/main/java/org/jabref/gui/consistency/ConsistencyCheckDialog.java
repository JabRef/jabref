package org.jabref.gui.consistency;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
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

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsistencyCheckDialog extends BaseDialog<Void> {

    private Logger LOGGER = LoggerFactory.getLogger(ConsistencyCheckDialog.class);

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
                message.message().split("\\s+")[0].equals(viewModel.selectedEntryTypeProperty().get())
        );

        viewModel.selectedEntryTypeProperty().addListener((obs, oldValue, newValue) -> {
            filteredData.setPredicate(message ->
                    message.message().split("\\s+")[0].equals(newValue)
            );
        });

        tableView.setItems(filteredData);

        for (int i = 0; i < viewModel.getColumnNames().size(); i++) {
            int columnIndex = i;
            TableColumn<ConsistencyMessage, String> tableColumn = new TableColumn<>(viewModel.getColumnNames().get(i));
            tableColumn.setCellValueFactory(row -> {
                String[] message = row.getValue().message().split("\\s+");
                return new ReadOnlyStringWrapper(message[columnIndex]);
            });
            tableView.getColumns().add(tableColumn);
        }
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
        dialogService.showInformationDialogAndWait(
                Localization.lang("Symbols Information"),
                Localization.lang("x    :    Field is present\n" +
                                  "o    :    Optional field is present\n" +
                                  "?    :    Unknown field is present\n" +
                                  "-    :    Field is absent"));
    }
}
