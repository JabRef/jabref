package org.jabref.gui.exporter;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import org.fxmisc.easybind.EasyBind;

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

    @FXML
    private void initialize() {
        viewModel = new ExportCustomizationDialogViewModel(dialogService, loader);
        //enable multiple selection somewhere around here
        EasyBind.listBind(viewModel.selectedExportersProperty(),
                          EasyBind.monadic(exporterTable.selectionModelProperty().

                              )
        //trying something new above
        viewModel.selectedExportersProperty().bind(
                EasyBind.monadic(exporterTable.selectionModelProperty()).
                flatMap(SelectionModel::selectedItemsProperty). //This will have to be done differently from key bindings because it's multiple selection
                selectProperty(Item::valueProperty)
        );

    }

}