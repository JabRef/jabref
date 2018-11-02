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
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

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
    @Inject private JournalAbbreviationLoader loader; //not sure this should be injected this way
    private ExportCustomizationDialogViewModel viewModel;

    public ExportCustomizationDialogView() {
        this.setTitle(Localization.lang("Customize Export Formats"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(addButton, getDialogPane(), event -> addExporter());
        ControlHelper.setAction(modifyButton, getDialogPane(), event -> modifyExporter());
        ControlHelper.setAction(removeButton, getDialogPane(), event -> removeExporter());
    }

    private void removeExporter() {
        // TODO Auto-generated method stub
    }

    private void modifyExporter() {
        // TODO Auto-generated method stub
    }

    private void addExporter() {
        // TODO Auto-generated method stub

    }

    @FXML
    private void initialize() {
        viewModel = new ExportCustomizationDialogViewModel(dialogService, loader);
        //enable multiple selection somewhere around here

        exporterTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        exporterTable.setItems(viewModel.selectedExportersProperty());

    }

}