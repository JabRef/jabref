package org.jabref.gui.preferences;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import org.jabref.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.bibtexkeypattern.BibtexKeyPatternTableView;
import org.jabref.gui.help.HelpAction;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class BibtexKeyPatternTabView extends AbstractPreferenceTabView<BibtexKeyPatternTabViewModel> implements PreferencesTab {

    @FXML public CheckBox overwriteAllow;
    @FXML public CheckBox overwriteWarning;
    @FXML public CheckBox generateOnSave;
    @FXML public RadioButton letterStartA;
    @FXML public RadioButton letterStartB;
    @FXML public RadioButton letterAlwaysAdd;
    @FXML public TextField keyPatternRegex;
    @FXML public TextField keyPatternReplacement;
    @FXML public HBox keyPatternContainer;
    @FXML public Button keyPatternHelp;

    private final BibtexKeyPatternTableView bibtexKeyPatternTable;

    public BibtexKeyPatternTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        bibtexKeyPatternTable = new BibtexKeyPatternTableView(preferences,
                Globals.entryTypesManager.getAllTypes(preferences.getDefaultBibDatabaseMode()),
                preferences.getKeyPattern());

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() { return Localization.lang("BibTeX key generator"); }

    public void initialize () {
        this.viewModel = new BibtexKeyPatternTabViewModel(dialogService, preferences);

        overwriteAllow.selectedProperty().bindBidirectional(viewModel.overwriteAllowProperty());
        overwriteWarning.selectedProperty().bindBidirectional(viewModel.overwriteWarningProperty());
        generateOnSave.selectedProperty().bindBidirectional(viewModel.generateOnSaveProperty());
        letterStartA.selectedProperty().bindBidirectional(viewModel.letterStartAProperty());
        letterStartB.selectedProperty().bindBidirectional(viewModel.letterStartBProperty());
        letterAlwaysAdd.selectedProperty().bindBidirectional(viewModel.letterAlwaysAddProperty());
        keyPatternRegex.textProperty().bindBidirectional(viewModel.keyPatternRegexProperty());
        keyPatternReplacement.textProperty().bindBidirectional(viewModel.keyPatternReplacementProperty());

        bibtexKeyPatternTable.setPrefWidth(650.0);
        bibtexKeyPatternTable.patternListProperty().bindBidirectional(viewModel.patternListProperty());
        bibtexKeyPatternTable.defaultKeyPatternProperty().bindBidirectional(viewModel.defaultKeyPatternProperty());
        keyPatternContainer.getChildren().add(bibtexKeyPatternTable);

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP_KEY_PATTERNS, new HelpAction(HelpFile.BIBTEX_KEY_PATTERN), keyPatternHelp);
    }

    @Override
    public void setValues() {
        viewModel.setValues();
        bibtexKeyPatternTable.setValues();
    }

    @Override
    public void storeSettings() {
        viewModel.storeSettings();
    }

    @FXML
    public void resetAllKeyPatterns() { bibtexKeyPatternTable.resetAll(); }
}
