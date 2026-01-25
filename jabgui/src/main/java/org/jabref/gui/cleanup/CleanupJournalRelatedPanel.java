package org.jabref.gui.cleanup;

import java.util.EnumSet;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupTabSelection;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import org.jspecify.annotations.NonNull;

public class CleanupJournalRelatedPanel extends VBox implements CleanupPanel {
    @FXML public Label cleanupJournalAbbreviationsLabel;
    @FXML private ToggleGroup journalAbbreviationsToggleGroup;

    @FXML private RadioButton abbreviateDefault;
    @FXML private RadioButton abbreviateDottles;
    @FXML private RadioButton abbreviateShortestUnique;
    @FXML private RadioButton abbreviateLTWA;
    @FXML private RadioButton unabbreviate;

    private final CleanupJournalRelatedViewModel viewModel;
    private final CleanupDialogViewModel dialogViewModel;

    public CleanupJournalRelatedPanel(@NonNull CleanupPreferences cleanupPreferences,
                                      @NonNull CleanupDialogViewModel dialogViewModel) {
        this.dialogViewModel = dialogViewModel;
        this.viewModel = new CleanupJournalRelatedViewModel(cleanupPreferences);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        initialize();
        bindProperties();
    }

    private void initialize() {
        cleanupJournalAbbreviationsLabel.setText(Localization.lang("Manage journal abbreviations"));

        abbreviateDefault.setText(Localization.lang("Abbreviate (default)"));
        abbreviateDefault.setUserData(CleanupPreferences.CleanupStep.ABBREVIATE_DEFAULT);

        abbreviateDottles.setText(Localization.lang("Abbreviate (dotless)"));
        abbreviateDottles.setUserData(CleanupPreferences.CleanupStep.ABBREVIATE_DOTLESS);

        abbreviateShortestUnique.setText(Localization.lang("Abbreviate (shortest unique)"));
        abbreviateShortestUnique.setUserData(CleanupPreferences.CleanupStep.ABBREVIATE_SHORTEST_UNIQUE);

        abbreviateLTWA.setText(Localization.lang("Abbreviate (LTWA)"));
        abbreviateLTWA.setUserData(CleanupPreferences.CleanupStep.ABBREVIATE_LTWA);

        unabbreviate.setText(Localization.lang("Unabbreviate"));
        unabbreviate.setUserData(CleanupPreferences.CleanupStep.UNABBREVIATE);
    }

    private void bindProperties() {
        journalAbbreviationsToggleGroup.selectToggle(
                journalAbbreviationsToggleGroup.getToggles().stream()
                                               .filter(toggle -> toggle.getUserData().equals(viewModel.selectedJournalCleanupOption.get()))
                                               .findFirst().orElse(null));
        journalAbbreviationsToggleGroup.selectedToggleProperty().addListener((_, _, newToggle) -> {
            if (newToggle != null) {
                CleanupPreferences.CleanupStep step = (CleanupPreferences.CleanupStep) newToggle.getUserData();
                viewModel.selectedJournalCleanupOption.set(step);
            }
        });
    }

    @Override
    public CleanupTabSelection getSelectedTab() {
        EnumSet<CleanupPreferences.CleanupStep> selectedMethods = EnumSet.noneOf(CleanupPreferences.CleanupStep.class);

        CleanupPreferences.CleanupStep selected = viewModel.selectedJournalCleanupOption.get();
        selectedMethods.add(selected);

        return CleanupTabSelection.ofJobs(CleanupJournalRelatedViewModel.CLEANUP_JOURNAL_METHODS, selectedMethods);
    }
}
