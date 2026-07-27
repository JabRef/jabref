package org.jabref.gui.preferences.customexporter;

import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.exporter.ExporterViewModel;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;

import com.tobiasdiez.easybind.EasyBind;

import static org.jabref.gui.preferences.forms.FormMetrics.BUTTON_WIDTH;

public class CustomExporterTab extends AbstractPreferenceTabView<CustomExporterTabViewModel> {

    public CustomExporterTab() {
        viewModel = new CustomExporterTabViewModel(preferences.getExportPreferences(), dialogService);
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Custom export formats");
    }

    private void buildView() {
        setContent(form()
                .table(buildExporterTable())
                .buttonRow(
                        ControlHelper.labelledIconButton(IconTheme.JabRefIcons.ADD_NOBOX, Localization.lang("Add"), BUTTON_WIDTH, viewModel::addExporter),
                        ControlHelper.labelledIconButton(IconTheme.JabRefIcons.EDIT, Localization.lang("Modify"), BUTTON_WIDTH, viewModel::modifyExporter),
                        ControlHelper.labelledIconButton(IconTheme.JabRefIcons.REMOVE_NOBOX, Localization.lang("Remove"), BUTTON_WIDTH, viewModel::removeExporters))
                .build());
    }

    private TableView<ExporterViewModel> buildExporterTable() {
        TableView<ExporterViewModel> exporterTable = new TableView<>();
        exporterTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        exporterTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        exporterTable.itemsProperty().bind(viewModel.exportersProperty());
        EasyBind.bindContent(viewModel.selectedExportersProperty(), exporterTable.getSelectionModel().getSelectedItems());

        TableColumn<ExporterViewModel, String> nameColumn = new TableColumn<>(Localization.lang("Export name"));
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().name());

        TableColumn<ExporterViewModel, String> layoutColumn = new TableColumn<>(Localization.lang("Main layout file"));
        layoutColumn.setCellValueFactory(cellData -> cellData.getValue().layoutFileName());

        TableColumn<ExporterViewModel, String> extensionColumn = new TableColumn<>(Localization.lang("Extension"));
        extensionColumn.setCellValueFactory(cellData -> cellData.getValue().extension());

        exporterTable.getColumns().add(nameColumn);
        exporterTable.getColumns().add(layoutColumn);
        exporterTable.getColumns().add(extensionColumn);
        return exporterTable;
    }
}
