package org.jabref.gui.preferences.customization;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class CustomizationTab extends AbstractPreferenceTabView<CustomizationTabViewModel> implements PreferencesTab {

    @FXML private CheckBox useCustomDOI;
    @FXML private TextField useCustomDOIName;

    public CustomizationTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Customization");
    }

    public void initialize() {
        this.viewModel = new CustomizationTabViewModel(dialogService, preferencesService);

        useCustomDOI.selectedProperty().bindBidirectional(viewModel.useCustomDOIProperty());
        useCustomDOIName.textProperty().bindBidirectional(viewModel.useCustomDOINameProperty());
        useCustomDOIName.disableProperty().bind(useCustomDOI.selectedProperty().not());
    }
}
