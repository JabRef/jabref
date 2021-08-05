package org.jabref.gui.preferences.importer;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class ImportSettingsTab extends AbstractPreferenceTabView<ImportSettingsTabViewModel> implements PreferencesTab {

    @FXML private CheckBox generateNewKeyOnImport;

    public ImportSettingsTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Import settings");
    }

    public void initialize() {
        this.viewModel = new ImportSettingsTabViewModel(preferencesService);
        generateNewKeyOnImport.selectedProperty().bindBidirectional(viewModel.generateKeyOnImportProperty());
    }

}
