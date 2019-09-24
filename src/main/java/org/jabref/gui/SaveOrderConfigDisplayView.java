package org.jabref.gui;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import org.jabref.gui.util.FieldsUtil;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class SaveOrderConfigDisplayView extends VBox {

    @FXML private ToggleGroup saveOrderToggleGroup;
    @FXML private RadioButton exportInSpecifiedOrder;
    @FXML private RadioButton exportInTableOrder;
    @FXML private RadioButton exportInOriginalOrder;
    @FXML private ComboBox<Field> savePriSort;
    @FXML private ComboBox<Field> saveSecSort;
    @FXML private ComboBox<Field> saveTerSort;
    @FXML private CheckBox savePriDesc;
    @FXML private CheckBox saveSecDesc;
    @FXML private CheckBox saveTerDesc;
    @Inject private PreferencesService preferencesService;

    private SaveOrderConfigDisplayViewModel viewModel;

    public SaveOrderConfigDisplayView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {

        viewModel = new SaveOrderConfigDisplayViewModel(preferencesService);

        exportInOriginalOrder.selectedProperty().bindBidirectional(viewModel.saveInOriginalProperty());
        exportInTableOrder.selectedProperty().bindBidirectional(viewModel.saveInTableOrderProperty());
        exportInSpecifiedOrder.selectedProperty().bindBidirectional(viewModel.saveInSpecifiedOrderProperty());

        new ViewModelListCellFactory<Field>()
                .withText(FieldsUtil::getNameWithType)
                .install(savePriSort);
        savePriSort.itemsProperty().bindBidirectional(viewModel.priSortFieldsProperty());
        savePriSort.valueProperty().bindBidirectional(viewModel.savePriSortSelectedValueProperty());
        savePriSort.setConverter(FieldsUtil.fieldStringConverter);

        new ViewModelListCellFactory<Field>()
                .withText(FieldsUtil::getNameWithType)
                .install(saveSecSort);
        saveSecSort.itemsProperty().bindBidirectional(viewModel.secSortFieldsProperty());
        saveSecSort.valueProperty().bindBidirectional(viewModel.saveSecSortSelectedValueProperty());
        saveSecSort.setConverter(FieldsUtil.fieldStringConverter);

        new ViewModelListCellFactory<Field>()
                .withText(FieldsUtil::getNameWithType)
                .install(saveTerSort);
        saveTerSort.itemsProperty().bindBidirectional(viewModel.terSortFieldsProperty());
        saveTerSort.valueProperty().bindBidirectional(viewModel.saveTerSortSelectedValueProperty());
        saveTerSort.setConverter(FieldsUtil.fieldStringConverter);

        savePriDesc.selectedProperty().bindBidirectional(viewModel.savePriDescPropertySelected());
        saveSecDesc.selectedProperty().bindBidirectional(viewModel.saveSecDescPropertySelected());
        saveTerDesc.selectedProperty().bindBidirectional(viewModel.saveTerDescPropertySelected());

    }

    public void setValues(SaveOrderConfig config) {
        viewModel.setSaveOrderConfig(config);
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
