package org.jabref.gui;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

import org.jabref.model.metadata.MetaData;

public class SaveOrderConfigDisplayView {

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
    private SaveOrderConfigDisplayViewModel viewModel;
    private final MetaData metadata;

    public SaveOrderConfigDisplayView(MetaData metadata) {
        this.metadata = metadata;
    }

    @FXML
    private void initialize() {

        viewModel = new SaveOrderConfigDisplayViewModel(metadata);

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

    public void storeConfig() {
        viewModel.storeConfig();
    }
}
