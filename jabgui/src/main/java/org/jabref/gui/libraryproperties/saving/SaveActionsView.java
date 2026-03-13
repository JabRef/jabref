package org.jabref.gui.libraryproperties.saving;

import javafx.fxml.FXML;

import org.jabref.gui.cleanup.JournalAbbreviationPanel;
import org.jabref.gui.cleanup.MultiFieldsCleanupPanel;
import org.jabref.gui.commonfxcontrols.FieldFormatterCleanupsPanel;
import org.jabref.gui.libraryproperties.AbstractPropertiesTabView;
import org.jabref.gui.libraryproperties.PropertiesTab;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class SaveActionsView extends AbstractPropertiesTabView<SaveActionsViewModel> implements PropertiesTab {

    @FXML private FieldFormatterCleanupsPanel fieldFormatterCleanupsPanel;
    @FXML private MultiFieldsCleanupPanel multiFieldsCleanupPanel;
    @FXML private JournalAbbreviationPanel journalAbbreviationPanel;

    @Inject private CliPreferences preferences;

    public SaveActionsView(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Save actions");
    }

    public void initialize() {
        this.viewModel = new SaveActionsViewModel(databaseContext, preferences);

        fieldFormatterCleanupsPanel.cleanupsProperty().bindBidirectional(viewModel.fieldFormatterCleanupsProperty());
        multiFieldsCleanupPanel.selectedJobsProperty().bindBidirectional(viewModel.multiFieldCleanupsPropertyProperty());
        journalAbbreviationPanel.selectedJournalCleanupOption().bindBidirectional(viewModel.journalAbbreviationCleanupPropertyProperty());
        fieldFormatterCleanupsPanel.cleanupsDisableProperty().bindBidirectional(viewModel.fieldFormatterCleanupsDisableProperty());
    }
}
