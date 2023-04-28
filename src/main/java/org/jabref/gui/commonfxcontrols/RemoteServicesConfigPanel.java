package org.jabref.gui.commonfxcontrols;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.VBox;


public class
RemoteServicesConfigPanel extends VBox {
    @FXML private RadioButton gorbidEnabled;
    @FXML private RadioButton gorbidDisabled;
    private RemoteServicesConfigPanelViewModel viewModel;

    public RemoteServicesConfigPanel() {
        ViewLoader.view(this)
                .root(this)
                .load();
    }

    @FXML
    private void initialize() {
        viewModel = new RemoteServicesConfigPanelViewModel();
        gorbidEnabled.selectedProperty().bindBidirectional(viewModel.gorbidEnabledProperty());
        gorbidDisabled.selectedProperty().bindBidirectional(viewModel.gorbidDisabledProperty());
    }
    public BooleanProperty gorbidEnabledProperty() {
        return viewModel.gorbidEnabledProperty();
    }
    public BooleanProperty gorbidDisabledProperty() {
        return viewModel.gorbidDisabledProperty();
    }




}
