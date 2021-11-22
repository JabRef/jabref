package org.jabref.gui.libraryproperties.keypattern;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.commonfxcontrols.CitationKeyPatternPanel;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.libraryproperties.AbstractPropertiesTabView;
import org.jabref.gui.libraryproperties.PropertiesTab;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class KeyPatternPropertiesView extends AbstractPropertiesTabView<KeyPatternPropertiesViewModel> implements PropertiesTab {

    @FXML private Button keyPatternHelp;
    @FXML private CitationKeyPatternPanel bibtexKeyPatternTable;

    @Inject private PreferencesService preferencesService;
    @Inject private BibEntryTypesManager bibEntryTypesManager;

    public KeyPatternPropertiesView(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Citation key patterns");
    }

    public void initialize() {
        this.viewModel = new KeyPatternPropertiesViewModel(databaseContext, preferencesService);

        bibtexKeyPatternTable.patternListProperty().bindBidirectional(viewModel.patternListProperty());
        bibtexKeyPatternTable.defaultKeyPatternProperty().bindBidirectional(viewModel.defaultKeyPatternProperty());

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP_KEY_PATTERNS, new HelpAction(HelpFile.CITATION_KEY_PATTERN), keyPatternHelp);
    }

    @Override
    public void setValues() {
        viewModel.setValues();
        bibtexKeyPatternTable.setValues(
                bibEntryTypesManager.getAllTypes(databaseContext.getMetaData().getMode()
                                                                .orElse(preferencesService.getGeneralPreferences()
                                                                                          .getDefaultBibDatabaseMode())),
                databaseContext.getMetaData().getCiteKeyPattern(preferencesService.getGlobalCitationKeyPattern()));
    }

    @FXML
    public void resetAllKeyPatterns() {
        bibtexKeyPatternTable.resetAll();
    }
}
