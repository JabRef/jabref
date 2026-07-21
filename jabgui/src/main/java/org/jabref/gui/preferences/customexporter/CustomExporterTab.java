package org.jabref.gui.preferences.customexporter;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import org.jabref.gui.exporter.ExporterViewModel;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.logic.l10n.Localization;

import com.tobiasdiez.easybind.EasyBind;

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
        getChildren().add(form()
                .title(Localization.lang("Custom export formats"))
                .custom(buildExporterTable())
                .custom(buildButtonRow())
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

    private Node buildButtonRow() {
        HBox row = new HBox(10.0,
                iconButton(Localization.lang("Add"), IconTheme.JabRefIcons.ADD_NOBOX, viewModel::addExporter),
                iconButton(Localization.lang("Modify"), IconTheme.JabRefIcons.EDIT, viewModel::modifyExporter),
                iconButton(Localization.lang("Remove"), IconTheme.JabRefIcons.REMOVE_NOBOX, viewModel::removeExporters));
        row.setAlignment(Pos.BASELINE_RIGHT);
        return row;
    }

    private Button iconButton(String text, IconTheme.JabRefIcons icon, Runnable action) {
        Button button = new Button(text);
        button.setPrefWidth(100.0);
        button.setGraphic(icon.getGraphicNode());
        button.setOnAction(_ -> action.run());
        return button;
    }
}
