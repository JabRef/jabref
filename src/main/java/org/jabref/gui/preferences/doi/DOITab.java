package org.jabref.gui.preferences.doi;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;

import com.airhacks.afterburner.views.ViewLoader;

public class DOITab extends AbstractPreferenceTabView<DOITabViewModel> implements PreferencesTab {

    @FXML private CheckBox useCustomDOI;
    @FXML private TextField useCustomDOIName;

    public DOITab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }


    @Override
    public String getTabName() {
        return "DOI";
    }

    public void initialize() {
        this.viewModel = new DOITabViewModel(dialogService, preferencesService);

        useCustomDOI.selectedProperty().bindBidirectional(viewModel.useCustomDOIProperty());
        useCustomDOIName.textProperty().bindBidirectional(viewModel.useCustomDOINameProperty());
        useCustomDOIName.disableProperty().bind(useCustomDOI.selectedProperty().not());
    }

}
