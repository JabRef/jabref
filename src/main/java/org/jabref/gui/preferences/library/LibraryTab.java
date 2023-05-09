package org.jabref.gui.preferences.library;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class LibraryTab extends AbstractPreferenceTabView<LibraryTabViewModel> implements PreferencesTab {

    @FXML private CheckBox collectTelemetry;

    public LibraryTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Library");
    }

    public void initialize() {
        this.viewModel = new LibraryTabViewModel(dialogService, preferencesService.getGeneralPreferences(), preferencesService.getTelemetryPreferences());

        collectTelemetry.selectedProperty().bindBidirectional(viewModel.collectTelemetryProperty());
    }
}
