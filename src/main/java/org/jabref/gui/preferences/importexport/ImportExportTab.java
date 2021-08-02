package org.jabref.gui.preferences.importexport;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class ImportExportTab extends AbstractPreferenceTabView<ImportExportTabViewModel> implements PreferencesTab {

    @FXML private CheckBox generateNewKeyOnImport;
    @FXML private CheckBox useCustomDOI;
    @FXML private TextField useCustomDOIName;

    public ImportExportTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Import and Export");
    }

    public void initialize() {
        this.viewModel = new ImportExportTabViewModel(preferencesService);

        useCustomDOI.selectedProperty().bindBidirectional(viewModel.useCustomDOIProperty());
        useCustomDOIName.textProperty().bindBidirectional(viewModel.useCustomDOINameProperty());
        useCustomDOIName.disableProperty().bind(useCustomDOI.selectedProperty().not());

        generateNewKeyOnImport.selectedProperty().bindBidirectional(viewModel.generateKeyOnImportProperty());
    }
}
