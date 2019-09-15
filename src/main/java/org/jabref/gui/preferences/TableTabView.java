package org.jabref.gui.preferences;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;

import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class TableTabView extends AbstractPreferenceTabView<TableTabViewModel> implements PreferencesTab {

    @FXML private CheckBox autoResizeName;
    @FXML private RadioButton namesNatbib;
    @FXML private RadioButton nameAsIs;
    @FXML private RadioButton nameFirstLast;
    @FXML private RadioButton nameLastFirst;
    @FXML private RadioButton abbreviationDisabled;
    @FXML private RadioButton abbreviationEnabled;
    @FXML private RadioButton abbreviationLastNameOnly;

    public TableTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    @Override
    public String getTabName() { return Localization.lang("Entry table"); }

    public void initialize () {
        this.viewModel = new TableTabViewModel(dialogService, preferences);

        autoResizeName.selectedProperty().bindBidirectional(viewModel.autoResizeNameProperty());
        namesNatbib.selectedProperty().bindBidirectional(viewModel.namesNatbibProperty());
        nameAsIs.selectedProperty().bindBidirectional(viewModel.nameAsIsProperty());
        nameFirstLast.selectedProperty().bindBidirectional(viewModel.nameFirstLastProperty());
        nameLastFirst.selectedProperty().bindBidirectional(viewModel.nameLastFirstProperty());
        abbreviationDisabled.selectedProperty().bindBidirectional(viewModel.abbreviationDisabledProperty());
        abbreviationEnabled.selectedProperty().bindBidirectional(viewModel.abbreviationEnabledProperty());
        abbreviationLastNameOnly.selectedProperty().bindBidirectional(viewModel.abbreviationLastNameOnlyProperty());
    }
}
