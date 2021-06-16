package org.jabref.gui.commonfxcontrols;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.VBox;

import org.jabref.gui.util.FieldsUtil;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;

public class SaveOrderConfigPanel extends VBox {

    @FXML private RadioButton exportInSpecifiedOrder;
    @FXML private RadioButton exportInTableOrder;
    @FXML private RadioButton exportInOriginalOrder;
    @FXML private ComboBox<Field> savePriSort;
    @FXML private ComboBox<Field> saveSecSort;
    @FXML private ComboBox<Field> saveTerSort;
    @FXML private CheckBox savePriDesc;
    @FXML private CheckBox saveSecDesc;
    @FXML private CheckBox saveTerDesc;

    private SaveOrderConfigPanelViewModel viewModel;

    public SaveOrderConfigPanel() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new SaveOrderConfigPanelViewModel();

        exportInOriginalOrder.selectedProperty().bindBidirectional(viewModel.saveInOriginalProperty());
        exportInTableOrder.selectedProperty().bindBidirectional(viewModel.saveInTableOrderProperty());
        exportInSpecifiedOrder.selectedProperty().bindBidirectional(viewModel.saveInSpecifiedOrderProperty());

        new ViewModelListCellFactory<Field>()
                .withText(FieldsUtil::getNameWithType)
                .install(savePriSort);
        savePriSort.itemsProperty().bindBidirectional(viewModel.primarySortFieldsProperty());
        savePriSort.valueProperty().bindBidirectional(viewModel.savePrimarySortSelectedValueProperty());
        savePriSort.setConverter(FieldsUtil.fieldStringConverter);

        new ViewModelListCellFactory<Field>()
                .withText(FieldsUtil::getNameWithType)
                .install(saveSecSort);
        saveSecSort.itemsProperty().bindBidirectional(viewModel.secondarySortFieldsProperty());
        saveSecSort.valueProperty().bindBidirectional(viewModel.saveSecondarySortSelectedValueProperty());
        saveSecSort.setConverter(FieldsUtil.fieldStringConverter);

        new ViewModelListCellFactory<Field>()
                .withText(FieldsUtil::getNameWithType)
                .install(saveTerSort);
        saveTerSort.itemsProperty().bindBidirectional(viewModel.tertiarySortFieldsProperty());
        saveTerSort.valueProperty().bindBidirectional(viewModel.saveTertiarySortSelectedValueProperty());
        saveTerSort.setConverter(FieldsUtil.fieldStringConverter);

        savePriDesc.selectedProperty().bindBidirectional(viewModel.savePrimaryDescPropertySelected());
        saveSecDesc.selectedProperty().bindBidirectional(viewModel.saveSecondaryDescPropertySelected());
        saveTerDesc.selectedProperty().bindBidirectional(viewModel.saveTertiaryDescPropertySelected());
    }

    public BooleanProperty saveInOriginalProperty() {
        return viewModel.saveInOriginalProperty();
    }

    public BooleanProperty saveInTableOrderProperty() {
        return viewModel.saveInTableOrderProperty();
    }

    public BooleanProperty saveInSpecifiedOrderProperty() {
        return viewModel.saveInSpecifiedOrderProperty();
    }

    public ListProperty<Field> primarySortFieldsProperty() {
        return viewModel.primarySortFieldsProperty();
    }

    public ListProperty<Field> secondarySortFieldsProperty() {
        return viewModel.secondarySortFieldsProperty();
    }

    public ListProperty<Field> tertiarySortFieldsProperty() {
        return viewModel.tertiarySortFieldsProperty();
    }

    public ObjectProperty<Field> savePrimarySortSelectedValueProperty() {
        return viewModel.savePrimarySortSelectedValueProperty();
    }

    public ObjectProperty<Field> saveSecondarySortSelectedValueProperty() {
        return viewModel.saveSecondarySortSelectedValueProperty();
    }

    public ObjectProperty<Field> saveTertiarySortSelectedValueProperty() {
        return viewModel.saveTertiarySortSelectedValueProperty();
    }

    public BooleanProperty savePrimaryDescPropertySelected() {
        return viewModel.savePrimaryDescPropertySelected();
    }

    public BooleanProperty saveSecondaryDescPropertySelected() {
        return viewModel.saveSecondaryDescPropertySelected();
    }

    public BooleanProperty saveTertiaryDescPropertySelected() {
        return viewModel.saveTertiaryDescPropertySelected();
    }
}
