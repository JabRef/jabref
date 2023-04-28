package org.jabref.gui.preferences.shareddatabase;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class SharedDatabaseTab extends AbstractPreferenceTabView<SharedDatabaseTabViewModel> implements PreferencesTab {

    @FXML private CheckBox connectLastStartup;

    public SharedDatabaseTab() {
        ViewLoader.view(this)
                .root(this)
                .load();
    }

    public void initialize() {
        this.viewModel = new SharedDatabaseTabViewModel(preferencesService.getExternalApplicationsPreferences());
        this.connectLastStartup.selectedProperty().bindBidirectional(viewModel.connectLastStartupProperty());
    }

    @Override
    public String getTabName() {
        return Localization.lang("Shared Database");
    }
}
