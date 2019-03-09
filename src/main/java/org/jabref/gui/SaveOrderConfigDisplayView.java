package org.jabref.gui;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class SaveOrderConfigDisplayView extends GridPane {

    private final SaveOrderConfig config;

    @FXML private ToggleGroup saveOrderToggleGroup;
    @FXML private ComboBox<String> savePriSort;
    @FXML private ComboBox<String> saveSecSort;
    @FXML private ComboBox<String> saveTerSort;
    @FXML private RadioButton exportInSpecifiedOrder;
    @FXML private RadioButton exportInTableOrder;
    @FXML private RadioButton exportInOriginalOrder;
    @FXML private CheckBox savePriDesc;
    @FXML private CheckBox saveSecDesc;
    @FXML private CheckBox saveTerDesc;
    @Inject private PreferencesService preferencesService;

    private SaveOrderConfigDisplayViewModel viewModel;

    public SaveOrderConfigDisplayView(SaveOrderConfig config) {
        this.config = config;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {

        viewModel = new SaveOrderConfigDisplayViewModel(config, preferencesService);

        exportInSpecifiedOrder.selectedProperty().bindBidirectional(viewModel.saveInSpecifiedOrderProperty());
        exportInTableOrder.selectedProperty().bindBidirectional(viewModel.saveInTableOrderProperty());
        exportInOriginalOrder.selectedProperty().bindBidirectional(viewModel.saveInOriginalProperty());

        savePriSort.itemsProperty().bindBidirectional(viewModel.priSortFieldsProperty());
        saveSecSort.itemsProperty().bindBidirectional(viewModel.secSortFieldsProperty());
        saveTerSort.itemsProperty().bindBidirectional(viewModel.terSortFieldsProperty());

        savePriSort.valueProperty().bindBidirectional(viewModel.savePriSortSelectedValueProperty());
        saveSecSort.valueProperty().bindBidirectional(viewModel.saveSecSortSelectedValueProperty());
        saveTerSort.valueProperty().bindBidirectional(viewModel.saveTerSortSelectedValueProperty());

        savePriDesc.selectedProperty().bindBidirectional(viewModel.savePriDescPropertySelected());
        saveSecDesc.selectedProperty().bindBidirectional(viewModel.saveSecDescPropertySelected());
        saveTerDesc.selectedProperty().bindBidirectional(viewModel.saveTerDescPropertySelected());

    }

    public void changeExportDescriptionToSave() {
        exportInOriginalOrder.setText(Localization.lang("Save entries in their original order"));
        exportInSpecifiedOrder.setText(Localization.lang("Save entries ordered as specified"));
        exportInTableOrder.setText(Localization.lang("Save in current table sort order"));
    }

    public void storeConfig() {
        viewModel.storeConfigInPrefs();
    }

    public SaveOrderConfig getSaveOrderConfig() {
        return viewModel.getSaveOrderConfig();
    }

}
