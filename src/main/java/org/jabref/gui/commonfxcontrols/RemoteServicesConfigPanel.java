package org.jabref.gui.commonfxcontrols;

import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.VBox;

import com.airhacks.afterburner.views.ViewLoader;

public class RemoteServicesConfigPanel extends VBox {
    @FXML private RadioButton gorbidEnabled;
    @FXML private RadioButton gorbidDisabled;
    @FXML private RadioButton gorbidDemanded;
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
        gorbidDemanded.selectedProperty().bindBidirectional(viewModel.gorbidDemandedProperty());
    }

    public BooleanProperty gorbidEnabledProperty() {
        return viewModel.gorbidEnabledProperty();
    }

    public BooleanProperty gorbidDisabledProperty() {
        return viewModel.gorbidDisabledProperty();
    }

    public BooleanProperty gorbidDemandedProperty() {
        return viewModel.gorbidDemandedProperty();
    }
}
