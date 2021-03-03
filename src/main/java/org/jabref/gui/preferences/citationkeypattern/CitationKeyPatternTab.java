package org.jabref.gui.preferences.citationkeypattern;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.commonfxcontrols.CitationKeyPatternPanel;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class CitationKeyPatternTab extends AbstractPreferenceTabView<CitationKeyPatternTabViewModel> implements PreferencesTab {

    @FXML private CheckBox overwriteAllow;
    @FXML private CheckBox overwriteWarning;
    @FXML private CheckBox generateOnSave;
    @FXML private RadioButton letterStartA;
    @FXML private RadioButton letterStartB;
    @FXML private RadioButton letterAlwaysAdd;
    @FXML private TextField keyPatternRegex;
    @FXML private TextField keyPatternReplacement;
    @FXML private TextField unwantedCharacters;
    @FXML private Button keyPatternHelp;
    @FXML private CitationKeyPatternPanel bibtexKeyPatternTable;

    public CitationKeyPatternTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Citation key generator");
    }

    public void initialize() {
        this.viewModel = new CitationKeyPatternTabViewModel(dialogService, preferencesService);

        overwriteAllow.selectedProperty().bindBidirectional(viewModel.overwriteAllowProperty());
        overwriteWarning.selectedProperty().bindBidirectional(viewModel.overwriteWarningProperty());
        generateOnSave.selectedProperty().bindBidirectional(viewModel.generateOnSaveProperty());
        letterStartA.selectedProperty().bindBidirectional(viewModel.letterStartAProperty());
        letterStartB.selectedProperty().bindBidirectional(viewModel.letterStartBProperty());
        letterAlwaysAdd.selectedProperty().bindBidirectional(viewModel.letterAlwaysAddProperty());
        keyPatternRegex.textProperty().bindBidirectional(viewModel.keyPatternRegexProperty());
        keyPatternReplacement.textProperty().bindBidirectional(viewModel.keyPatternReplacementProperty());
        unwantedCharacters.textProperty().bindBidirectional(viewModel.unwantedCharactersProperty());

        bibtexKeyPatternTable.patternListProperty().bindBidirectional(viewModel.patternListProperty());
        bibtexKeyPatternTable.defaultKeyPatternProperty().bindBidirectional(viewModel.defaultKeyPatternProperty());

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP_KEY_PATTERNS, new HelpAction(HelpFile.CITATION_KEY_PATTERN), keyPatternHelp);
    }

    @Override
    public void setValues() {
        viewModel.setValues();
        bibtexKeyPatternTable.setValues(
                Globals.entryTypesManager.getAllTypes(preferencesService.getGeneralPreferences().getDefaultBibDatabaseMode()),
                preferencesService.getGlobalCitationKeyPattern());
    }

    @Override
    public void storeSettings() {
        viewModel.storeSettings();
    }

    @FXML
    public void resetAllKeyPatterns() {
        bibtexKeyPatternTable.resetAll();
    }
}
