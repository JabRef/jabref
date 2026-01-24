package org.jabref.gui.cleanup;

import java.util.EnumSet;

import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupTabSelection;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import org.jspecify.annotations.NonNull;

public class CleanupJournalRelatedPanel extends VBox implements CleanupPanel {
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

        initializeComboBox();
        bindProperties();
    }

    private void initializeComboBox() {
        abbreviateDefault.setText(Localization.lang("Abbreviate (default)"));
        abbreviateDottles.setText(Localization.lang("Abbreviate (dotless)"));
        abbreviateShortestUnique.setText(Localization.lang("Abbreviate (shortest unique)"));
        abbreviateLTWA.setText(Localization.lang("Abbreviate (LTWA)"));
        unabbreviate.setText(Localization.lang("Unabbreviate"));

        abbreviateDefault.setUserData(CleanupPreferences.CleanupStep.ABBREVIATE_DEFAULT);
        abbreviateDottles.setUserData(CleanupPreferences.CleanupStep.ABBREVIATE_DOTLESS);
        abbreviateShortestUnique.setUserData(CleanupPreferences.CleanupStep.ABBREVIATE_SHORTEST_UNIQUE);
        abbreviateLTWA.setUserData(CleanupPreferences.CleanupStep.ABBREVIATE_LTWA);
        unabbreviate.setUserData(CleanupPreferences.CleanupStep.UNABBREVIATE);
    }

    private void bindProperties() {
        journalAbbreviationsToggleGroup.selectToggle(journalAbbreviationsToggleGroup.getToggles().stream()
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
