package org.jabref.gui.consistency;

import java.util.Collection;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.logic.quality.consistency.ConsistencyMessage;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;

public class ConsistencyCheckDialog extends BaseDialog<Void> {

    @FXML private TableView<ConsistencyMessage> tableView;
    @FXML private ComboBox<String> entryTypeCombo;

    private final BibliographyConsistencyCheck.Result result;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private String selectedEntry;

    private ConsistencyCheckDialogViewModel viewModel;

    public ConsistencyCheckDialog(BibliographyConsistencyCheck.Result result, DialogService dialogService, GuiPreferences preferences) {
        this.result = result;
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
        viewModel = new ConsistencyCheckDialogViewModel(result, dialogService, preferences);

        result.entryTypeToResultMap().forEach((entrySet, entryTypeResult) -> {
            entryTypeCombo.getItems().add(entrySet.toString());

            Collection<Field> fields = entryTypeResult.fields();
            for (Field field: fields) {
                TableColumn<ConsistencyMessage, String> tableColumn = new TableColumn<>(field.toString());
                tableColumn.setCellValueFactory(new PropertyValueFactory<>(field.toString()));
                tableView.getColumns().add(tableColumn);
            }
        });

        entryTypeCombo.getSelectionModel().select(entryTypeCombo.getItems().getFirst());
    }

    public void selectEntry() {
        selectedEntry = entryTypeCombo.getSelectionModel().getSelectedItem();
    }

    public void exportAsCsv() {
        viewModel.startExportAsCsv();
    }

    public void exportAsTxt() {
        viewModel.startExportAsTxt();
    }
}

