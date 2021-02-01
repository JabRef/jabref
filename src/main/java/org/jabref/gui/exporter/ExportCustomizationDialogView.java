package org.jabref.gui.exporter;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

public class ExportCustomizationDialogView extends BaseDialog<Void> {

    @FXML private ButtonType addButton;
    @FXML private ButtonType modifyButton;
    @FXML private ButtonType removeButton;
    @FXML private ButtonType closeButton;
    @FXML private TableView<ExporterViewModel> exporterTable;
    @FXML private TableColumn<ExporterViewModel, String> nameColumn;
    @FXML private TableColumn<ExporterViewModel, String> layoutColumn;
    @FXML private TableColumn<ExporterViewModel, String> extensionColumn;

    @Inject private DialogService dialogService;
    @Inject private PreferencesService preferences;
    @Inject private JournalAbbreviationRepository repository;
    private ExportCustomizationDialogViewModel viewModel;

    public ExportCustomizationDialogView() {
        this.setTitle(Localization.lang("Customize Export Formats"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(addButton, getDialogPane(), event -> viewModel.addExporter());
        ControlHelper.setAction(modifyButton, getDialogPane(), event -> viewModel.modifyExporter());
        ControlHelper.setAction(removeButton, getDialogPane(), event -> viewModel.removeExporters());
        ControlHelper.setAction(closeButton, getDialogPane(), event -> {
            viewModel.saveToPrefs();
            close();
        });
    }

    @FXML
    private void initialize() {
        viewModel = new ExportCustomizationDialogViewModel(preferences, dialogService, repository);
        exporterTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        exporterTable.itemsProperty().bind(viewModel.exportersProperty());
        EasyBind.bindContent(viewModel.selectedExportersProperty(), exporterTable.getSelectionModel().getSelectedItems());
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().name());
        layoutColumn.setCellValueFactory(cellData -> cellData.getValue().layoutFileName());
        extensionColumn.setCellValueFactory(cellData -> cellData.getValue().extension());
    }
}
