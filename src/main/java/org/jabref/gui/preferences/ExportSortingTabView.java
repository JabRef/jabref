package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;

import org.jabref.gui.commonfxcontrols.SaveOrderConfigPanel;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class ExportSortingTabView extends AbstractPreferenceTabView<ExportSortingTabViewModel> implements PreferencesTab {

    @FXML private SaveOrderConfigPanel exportOrderPanel;

    public ExportSortingTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        this.viewModel = new ExportSortingTabViewModel(preferences);

        exportOrderPanel.saveInOriginalProperty().bindBidirectional(viewModel.saveInOriginalProperty());
        exportOrderPanel.saveInTableOrderProperty().bindBidirectional(viewModel.saveInTableOrderProperty());
        exportOrderPanel.saveInSpecifiedOrderProperty().bindBidirectional(viewModel.saveInSpecifiedOrderProperty());
        exportOrderPanel.primarySortFieldsProperty().bind(viewModel.primarySortFieldsProperty());
        exportOrderPanel.secondarySortFieldsProperty().bind(viewModel.secondarySortFieldsProperty());
        exportOrderPanel.tertiarySortFieldsProperty().bind(viewModel.tertiarySortFieldsProperty());
        exportOrderPanel.savePrimaryDescPropertySelected().bindBidirectional(viewModel.savePrimaryDescPropertySelected());
        exportOrderPanel.saveSecondaryDescPropertySelected().bindBidirectional(viewModel.saveSecondaryDescPropertySelected());
        exportOrderPanel.saveTertiaryDescPropertySelected().bindBidirectional(viewModel.saveTertiaryDescPropertySelected());
        exportOrderPanel.savePrimarySortSelectedValueProperty().bindBidirectional(viewModel.savePrimarySortSelectedValueProperty());
        exportOrderPanel.saveSecondarySortSelectedValueProperty().bindBidirectional(viewModel.saveSecondarySortSelectedValueProperty());
        exportOrderPanel.saveTertiarySortSelectedValueProperty().bindBidirectional(viewModel.saveTertiarySortSelectedValueProperty());
    }

    @Override
    public String getTabName() { return Localization.lang("Export sorting"); }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public List<String> getRestartWarnings() {
        return new ArrayList<>();
    }
}
