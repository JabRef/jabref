package org.jabref.gui.consistency;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.logic.quality.consistency.ConsistencyMessage;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.views.ViewLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsistencyCheckDialog extends BaseDialog<Void> {

    private final Logger LOGGER = LoggerFactory.getLogger(ConsistencyCheckDialog.class);

    @FXML private TableView<ConsistencyMessage> tableView;
    @FXML private ComboBox<String> entryTypeCombo;
    private final StringProperty selectedEntry = new SimpleStringProperty();

    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;
    private final BibliographyConsistencyCheck.Result result;

    private ConsistencyCheckDialogViewModel viewModel;

    public ConsistencyCheckDialog(DialogService dialogService,
                                  GuiPreferences preferences,
                                  BibEntryTypesManager entryTypesManager,
                                  BibliographyConsistencyCheck.Result result) {
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

    public ConsistencyCheckDialogViewModel getViewModel() {
        return viewModel;
    }

    @FXML
    public void initialize() {
        viewModel = new ConsistencyCheckDialogViewModel(dialogService, preferences, entryTypesManager, result);

        selectedEntry.set(viewModel.getEntryTypes().getFirst());

        entryTypeCombo.getItems().addAll(viewModel.getEntryTypes());
        entryTypeCombo.getSelectionModel().select(selectedEntry.getValue());

        tableView.setItems(viewModel.getTableData());

        for (int i = 0; i < viewModel.getColumnNames().size(); i++) {
            int columnIndex = i;
            TableColumn<ConsistencyMessage, String> tableColumn = new TableColumn<>(viewModel.getColumnNames().get(i));
            tableColumn.setCellValueFactory(row -> {
                String[] message = row.getValue().message().split("\\s+");
                LOGGER.info("info: " + message[columnIndex]);
                return new ReadOnlyStringWrapper(message[columnIndex]);
            });
            tableView.getColumns().add(tableColumn);
        }
    }

    @FXML
    private void selectEntry() {
        selectedEntry.set(entryTypeCombo.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void exportAsCsv() {
        viewModel.startExportAsCsv();
    }

    @FXML
    private void exportAsTxt() {
        viewModel.startExportAsTxt();
    }
}
