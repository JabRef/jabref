package org.jabref.gui.preferences.customexporter;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.exporter.ExporterViewModel;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

public class CustomExporterTab extends AbstractPreferenceTabView<CustomExporterTabViewModel> implements PreferencesTab {

    @FXML private TableView<ExporterViewModel> exporterTable;
    @FXML private TableColumn<ExporterViewModel, String> nameColumn;
    @FXML private TableColumn<ExporterViewModel, String> layoutColumn;
    @FXML private TableColumn<ExporterViewModel, String> extensionColumn;

    @Inject private JournalAbbreviationRepository repository;

    public CustomExporterTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Custom export formats");
    }

    @FXML
    private void initialize() {
        viewModel = new CustomExporterTabViewModel(preferencesService, dialogService, repository);

        exporterTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        exporterTable.itemsProperty().bind(viewModel.exportersProperty());
        EasyBind.bindContent(viewModel.selectedExportersProperty(), exporterTable.getSelectionModel().getSelectedItems());
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().name());
        layoutColumn.setCellValueFactory(cellData -> cellData.getValue().layoutFileName());
        extensionColumn.setCellValueFactory(cellData -> cellData.getValue().extension());
    }

    @FXML
    private void add() {
        viewModel.addExporter();
    }

    @FXML
    private void modify() {
        viewModel.modifyExporter();
    }

    @FXML
    private void remove() {
        viewModel.removeExporters();
    }
}
