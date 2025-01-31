package org.jabref.gui.consistency;

import java.util.List;

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
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.views.ViewLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsistencyCheckDialog extends BaseDialog<Void> {

    private final Logger LOGGER = LoggerFactory.getLogger(ConsistencyCheckDialog.class);

    @FXML private TableView<ConsistencyMessage> tableView;
    @FXML private ComboBox<String> entryTypeCombo;

    private final BibliographyConsistencyCheck.Result result;
    private final DialogService dialogService;
    private final GuiPreferences preferences;

    private final List<String> entryTypes;
    private final List<String> columns;
    private final List<String> citationKeys;
    private final StringProperty selectedEntry = new SimpleStringProperty();

    private ConsistencyCheckDialogViewModel viewModel;

    public ConsistencyCheckDialog(BibliographyConsistencyCheck.Result result, List<String> entryTypes, List<String> columns, List<String> citationKeys, DialogService dialogService, GuiPreferences preferences) {
        this.result = result;
        this.entryTypes = entryTypes;
        this.columns = columns;
        this.selectedEntry.setValue(entryTypes.getFirst());
        this.citationKeys = citationKeys;
        this.dialogService = dialogService;
        this.preferences = preferences;

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
        viewModel = new ConsistencyCheckDialogViewModel(result, citationKeys, dialogService, preferences);

        entryTypeCombo.getItems().addAll(entryTypes);
        entryTypeCombo.getSelectionModel().select(selectedEntry.getValue());

        TableColumn<ConsistencyMessage, String> keyColumn = new TableColumn<>(StandardField.KEY.toString());
        tableView.getColumns().add(keyColumn);

        for (String column: columns) {
            TableColumn<ConsistencyMessage, String> tableColumn = new TableColumn<>(column);
            tableView.getColumns().add(tableColumn);
        }
    }

    public void selectEntry() {
        selectedEntry.set(entryTypeCombo.getSelectionModel().getSelectedItem());
    }

    public void exportAsCsv() {
        viewModel.startExportAsCsv();
    }

    public void exportAsTxt() {
        viewModel.startExportAsTxt();
    }
}

