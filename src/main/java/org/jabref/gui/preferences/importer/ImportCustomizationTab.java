package org.jabref.gui.preferences.importer;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.importer.ImporterViewModel;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

public class ImportCustomizationTab extends AbstractPreferenceTabView<ImportCustomizationTabViewModel> implements PreferencesTab {

    @FXML private Button addButton;

    @FXML private TableView<ImporterViewModel> importerTable;
    @FXML private TableColumn<ImporterViewModel, String> nameColumn;
    @FXML private TableColumn<ImporterViewModel, String> classColumn;
    @FXML private TableColumn<ImporterViewModel, String> basePathColumn;

    @Inject private DialogService dialogService;
    @Inject private PreferencesService preferences;

    public ImportCustomizationTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new ImportCustomizationTabViewModel(preferences, dialogService);

        importerTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        importerTable.itemsProperty().bind(viewModel.importersProperty());
        EasyBind.bindContent(viewModel.selectedImportersProperty(), importerTable.getSelectionModel().getSelectedItems());
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().name());
        classColumn.setCellValueFactory(cellData -> cellData.getValue().className());
        basePathColumn.setCellValueFactory(cellData -> cellData.getValue().basePath());
        new ViewModelTableRowFactory<ImporterViewModel>()
                .withTooltip(importer -> importer.getLogic().getDescription())
                .install(importerTable);

        addButton.setTooltip(new Tooltip(
                Localization.lang("Add a (compiled) custom Importer class from a class path.")
                        + "\n" + Localization.lang("The path need not be on the classpath of JabRef.")));
    }

    @Override
    public String getTabName() {
        return Localization.lang("Manage custom imports");
    }

    @FXML
    private void add() {
        viewModel.addImporter();
    }

    @FXML
    private void remove() {
        viewModel.removeSelectedImporter();
    }
}
