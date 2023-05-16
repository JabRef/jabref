package org.jabref.gui.preferences.autocompletion;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class AutoCompletionTab extends AbstractPreferenceTabView<AutoCompletionTabViewModel> implements PreferencesTab {

    @FXML private CheckBox enableAutoComplete;
    @FXML private TextField autoCompleteFields;
    @FXML private RadioButton autoCompleteFirstLast;
    @FXML private RadioButton autoCompleteLastFirst;
    @FXML private RadioButton autoCompleteBoth;
    @FXML private RadioButton firstNameModeAbbreviated;
    @FXML private RadioButton firstNameModeFull;
    @FXML private RadioButton firstNameModeBoth;

    public AutoCompletionTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Autocompletion");
    }

    public void initialize() {
        viewModel = new AutoCompletionTabViewModel(preferencesService.getAutoCompletePreferences());

        enableAutoComplete.selectedProperty().bindBidirectional(viewModel.enableAutoCompleteProperty());
        autoCompleteFields.textProperty().bindBidirectional(viewModel.autoCompleteFieldsProperty());
        autoCompleteFirstLast.selectedProperty().bindBidirectional(viewModel.autoCompleteFirstLastProperty());
        autoCompleteLastFirst.selectedProperty().bindBidirectional(viewModel.autoCompleteLastFirstProperty());
        autoCompleteBoth.selectedProperty().bindBidirectional(viewModel.autoCompleteBothProperty());
        firstNameModeAbbreviated.selectedProperty().bindBidirectional(viewModel.firstNameModeAbbreviatedProperty());
        firstNameModeFull.selectedProperty().bindBidirectional(viewModel.firstNameModeFullProperty());
        firstNameModeBoth.selectedProperty().bindBidirectional(viewModel.firstNameModeBothProperty());
    }
}
