package org.jabref.gui.importer;

import javax.inject.Inject;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

public class ImportCustomizationDialog extends BaseDialog<Void> {

    @FXML private ButtonType addButton;
    @FXML private ButtonType removeButton;
    @FXML private ButtonType closeButton;
    @FXML private TableView<CustomImporter> importerTable;
    @FXML private TableColumn<CustomImporter, String> nameColumn;
    @FXML private TableColumn<CustomImporter, String> classColumn;
    @FXML private TableColumn<CustomImporter, String> basePathColumn;

    @Inject private DialogService dialogService;
    @Inject private PreferencesService preferences;
    private ImportCustomizationDialogViewModel viewModel;

    public ImportCustomizationDialog() {
        this.setTitle(Localization.lang("Manage custom imports"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ((Button) getDialogPane().lookupButton(addButton)).setTooltip(new Tooltip(
                Localization.lang("Add a (compiled) custom Importer class from a class path.")
                        + "\n" + Localization.lang("The path need not be on the classpath of JabRef.")));
        ControlHelper.setAction(addButton, getDialogPane(), event -> viewModel.addImporter());
        ControlHelper.setAction(removeButton, getDialogPane(), event -> viewModel.removeSelectedImporter());
        ControlHelper.setAction(closeButton, getDialogPane(), event -> {
            viewModel.saveToPrefs();
            close();
        });
    }

    @FXML
    private void initialize() {
        viewModel = new ImportCustomizationDialogViewModel(preferences, dialogService);
        importerTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        importerTable.itemsProperty().bind(viewModel.importersProperty());
        EasyBind.bindContent(viewModel.selectedImportersProperty(), importerTable.getSelectionModel().getSelectedItems());
        nameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getName()));
        classColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getClassName()));
        basePathColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getBasePath().toString()));
        new ViewModelTableRowFactory<CustomImporter>()
                .withTooltip(CustomImporter::getDescription)
                .install(importerTable);
    }
}
