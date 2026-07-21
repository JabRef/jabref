package org.jabref.gui.preferences.customimporter;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.importer.ImporterViewModel;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.l10n.Localization;

import com.tobiasdiez.easybind.EasyBind;

public class CustomImporterTab extends AbstractPreferenceTabView<CustomImporterTabViewModel> {

    public CustomImporterTab() {
        viewModel = new CustomImporterTabViewModel(
                preferences.getImporterPreferences(),
                preferences.getFilePreferences(),
                dialogService);
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Custom import formats");
    }

    private void buildView() {
        getChildren().add(form()
                .custom(buildImporterTable())
                .custom(buildButtonRow())
                .build());
    }

    private TableView<ImporterViewModel> buildImporterTable() {
        TableView<ImporterViewModel> importerTable = new TableView<>();
        importerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        importerTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        importerTable.itemsProperty().bind(viewModel.importersProperty());
        EasyBind.bindContent(viewModel.selectedImportersProperty(), importerTable.getSelectionModel().getSelectedItems());

        TableColumn<ImporterViewModel, String> nameColumn = new TableColumn<>(Localization.lang("Import name"));
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().name());

        TableColumn<ImporterViewModel, String> classColumn = new TableColumn<>(Localization.lang("Importer class"));
        classColumn.setCellValueFactory(cellData -> cellData.getValue().className());

        TableColumn<ImporterViewModel, String> basePathColumn = new TableColumn<>(Localization.lang("Contained in"));
        basePathColumn.setCellValueFactory(cellData -> cellData.getValue().basePath());

        importerTable.getColumns().add(nameColumn);
        importerTable.getColumns().add(classColumn);
        importerTable.getColumns().add(basePathColumn);

        new ViewModelTableRowFactory<ImporterViewModel>()
                .withTooltip(importer -> importer.getLogic().getDescription())
                .install(importerTable);

        return importerTable;
    }

    private Node buildButtonRow() {
        Button addButton = iconButton(Localization.lang("Add"), IconTheme.JabRefIcons.ADD_NOBOX, viewModel::addImporter);
        addButton.setTooltip(new Tooltip(
                Localization.lang("Add a (compiled) custom Importer class from a class path.")
                        + "\n" + Localization.lang("The path need not be on the classpath of JabRef.")));

        HBox row = new HBox(10.0,
                addButton,
                iconButton(Localization.lang("Remove"), IconTheme.JabRefIcons.REMOVE_NOBOX, viewModel::removeSelectedImporter));
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
