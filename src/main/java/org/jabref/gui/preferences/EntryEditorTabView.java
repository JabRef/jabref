package org.jabref.gui.preferences;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class EntryEditorTabView extends AbstractPreferenceTabView<EntryEditorTabViewModel> implements PreferencesTab {

    @FXML private CheckBox openOnNewEntry;
    @FXML private CheckBox defaultSource;
    @FXML private CheckBox enableRelatedArticlesTab;
    @FXML private CheckBox acceptRecommendations;
    @FXML private CheckBox enableLatexCitationsTab;
    @FXML private CheckBox enableValidation;
    @FXML private CheckBox enableAutoComplete;
    @FXML private CheckBox enableEmacsKeyBindings;
    @FXML private CheckBox enableEmacsRebindCA;
    @FXML private CheckBox enableEmacsRebindCF;
    @FXML private CheckBox enableEmacsRebindCN;
    @FXML private CheckBox enableEmacsRebindAU;
    @FXML private TextField autoCompleteFields;
    @FXML private RadioButton autoCompleteFirstLast;
    @FXML private RadioButton autoCompleteLastFirst;
    @FXML private RadioButton autoCompleteBoth;
    @FXML private RadioButton firstNameModeAbbreviated;
    @FXML private RadioButton firstNameModeFull;
    @FXML private RadioButton firstNameModeBoth;

    public EntryEditorTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    @Override
    public String getTabName() { return Localization.lang("Entry editor"); }

    public void initialize () {
        this.viewModel = new EntryEditorTabViewModel(dialogService, preferences);

        openOnNewEntry.selectedProperty().bindBidirectional(viewModel.openOnNewEntryProperty());
        defaultSource.selectedProperty().bindBidirectional(viewModel.defaultSourceProperty());
        enableRelatedArticlesTab.selectedProperty().bindBidirectional(viewModel.enableRelatedArticlesTabProperty());
        acceptRecommendations.selectedProperty().bindBidirectional(viewModel.acceptRecommendationsProperty());
        enableLatexCitationsTab.selectedProperty().bindBidirectional(viewModel.enableLatexCitationsTabProperty());
        enableValidation.selectedProperty().bindBidirectional(viewModel.enableValidationProperty());
        enableAutoComplete.selectedProperty().bindBidirectional(viewModel.enableAutoCompleteProperty());
        enableEmacsKeyBindings.selectedProperty().bindBidirectional(viewModel.enableEmacsKeyBindingsProperty());
        enableEmacsRebindCA.selectedProperty().bindBidirectional(viewModel.enableEmacsRebindCAProperty());
        enableEmacsRebindCF.selectedProperty().bindBidirectional(viewModel.enableEmacsRebindCFProperty());
        enableEmacsRebindCN.selectedProperty().bindBidirectional(viewModel.enableEmacsRebindCNProperty());
        enableEmacsRebindAU.selectedProperty().bindBidirectional(viewModel.enableEmacsRebindAUProperty());
        autoCompleteFields.textProperty().bindBidirectional(viewModel.autoCompleteFieldsProperty());
        autoCompleteFirstLast.selectedProperty().bindBidirectional(viewModel.autoCompleteFirstLastProperty());
        autoCompleteLastFirst.selectedProperty().bindBidirectional(viewModel.autoCompleteLastFirstProperty());
        autoCompleteBoth.selectedProperty().bindBidirectional(viewModel.autoCompleteBothProperty());
        firstNameModeAbbreviated.selectedProperty().bindBidirectional(viewModel.firstNameModeAbbreviatedProperty());
        firstNameModeFull.selectedProperty().bindBidirectional(viewModel.firstNameModeFullProperty());
        firstNameModeBoth.selectedProperty().bindBidirectional(viewModel.firstNameModeBothProperty());
    }
}
