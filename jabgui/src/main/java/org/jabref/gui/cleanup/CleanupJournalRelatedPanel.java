package org.jabref.gui.cleanup;

import java.util.EnumSet;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import org.jabref.gui.commonfxcontrols.JournalAbbreviationPanel;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupTabSelection;

import com.airhacks.afterburner.views.ViewLoader;
import org.jspecify.annotations.NonNull;

public class CleanupJournalRelatedPanel extends VBox implements CleanupPanel {
    @FXML private JournalAbbreviationPanel journalAbbreviationPanel;

    private final CleanupJournalRelatedViewModel viewModel;
    private final CleanupDialogViewModel dialogViewModel;

    public CleanupJournalRelatedPanel(@NonNull CleanupPreferences cleanupPreferences,
                                      @NonNull CleanupDialogViewModel dialogViewModel) {

        this.dialogViewModel = dialogViewModel;
        this.viewModel = new CleanupJournalRelatedViewModel(cleanupPreferences);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        bindProperties();
    }

    private void bindProperties() {
        journalAbbreviationPanel.selectedJournalCleanupOption().bindBidirectional(viewModel.selectedJournalCleanupOption);
    }

    @Override
    public CleanupTabSelection getSelectedTab() {
        EnumSet<CleanupPreferences.CleanupStep> selectedMethods = EnumSet.noneOf(CleanupPreferences.CleanupStep.class);

        CleanupPreferences.CleanupStep selected = viewModel.selectedJournalCleanupOption.get();
        if (selected != null) {
            selectedMethods.add(selected);
        }

        return CleanupTabSelection.ofJobs(viewModel.allSupportedJobs, selectedMethods);
    }
}
