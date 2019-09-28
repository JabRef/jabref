package org.jabref.gui.preferences;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import org.jabref.gui.BasePanel;
import org.jabref.gui.bibtexkeypattern.BibtexKeyPatternPanel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
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
    @FXML public Pane specialKeyPatterns;

    private final BibtexKeyPatternPanel bibtexKeyPatternPanel;

    public BibtexKeyPatternTabView(JabRefPreferences preferences, BasePanel basePanel) {
        this.preferences = preferences;

        bibtexKeyPatternPanel = new BibtexKeyPatternPanel(basePanel);

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

        specialKeyPatterns.getChildren().add(bibtexKeyPatternPanel);
    }

    @Override
    public void setValues() {
        viewModel.setValues();
        bibtexKeyPatternPanel.setValues(preferences.getKeyPattern());
        bibtexKeyPatternPanel.setDefaultPat(viewModel.defaultKeyProperty().getValue());
    }

    @Override
    public void storeSettings() {
        viewModel.defaultKeyProperty().setValue(bibtexKeyPatternPanel.getDefaultPat());
        viewModel.storeSettings();
        GlobalBibtexKeyPattern keyPatterns = bibtexKeyPatternPanel.getKeyPatternAsGlobalBibtexKeyPattern();
        preferences.putKeyPattern(keyPatterns);
    }
}
