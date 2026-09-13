package org.jabref.gui.libraryproperties.keypattern;

import javafx.fxml.FXML;

import org.jabref.gui.commonfxcontrols.CitationKeyPatternsPanel;
import org.jabref.gui.libraryproperties.AbstractPropertiesTabView;
import org.jabref.gui.libraryproperties.PropertiesTab;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.MetaData;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class KeyPatternPropertiesView extends AbstractPropertiesTabView<KeyPatternPropertiesViewModel> implements PropertiesTab {

    @FXML private CitationKeyPatternsPanel bibtexKeyPatternTable;

    @Inject private GuiPreferences preferences;
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
        this.viewModel = new KeyPatternPropertiesViewModel(preferences);

        bibtexKeyPatternTable.patternListProperty().bindBidirectional(viewModel.patternListProperty());
        bibtexKeyPatternTable.defaultKeyPatternProperty().bindBidirectional(viewModel.defaultKeyPatternProperty());
    }

    @Override
    public void setValues(MetaData metaData) {
        viewModel.setValues(metaData);
        bibtexKeyPatternTable.setValues(
                bibEntryTypesManager.getAllTypes(metaData.getMode()
                                                         .orElse(preferences.getLibraryPreferences()
                                                                            .getDefaultBibDatabaseMode())),
                metaData.getCiteKeyPatterns(preferences.getCitationKeyPatternPreferences().getKeyPatterns()));
    }

    @FXML
    public void resetAllKeyPatterns() {
        bibtexKeyPatternTable.resetAll();
    }
}
