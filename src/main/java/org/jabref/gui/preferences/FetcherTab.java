package org.jabref.gui.preferences;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class FetcherTab extends AbstractPreferenceTabView<FetcherTabViewModel> implements PreferencesTab {

    @FXML private CheckBox useWorldcatKey;
    @FXML private TextField worldcatKey;

    public FetcherTab(PreferencesService preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new FetcherTabViewModel(preferences);

        useWorldcatKey.selectedProperty().bindBidirectional(viewModel.getUseWorldcatKeyProperty());
        worldcatKey.textProperty().bindBidirectional(viewModel.getWorldcatKeyProperty());
    }

    @Override
    public String getTabName() {
        return Localization.lang("Fetcher API keys");
    }

    @FXML
    private void openWorldcatWebpage() {
        viewModel.openWorldcatWebpage();
    }
}
