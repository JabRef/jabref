package org.jabref.gui.commonfxcontrols;

import java.util.EnumSet;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class JournalAbbreviationPanel extends VBox {
    @FXML public Label cleanupJournalAbbreviationsLabel;
    @FXML private ToggleGroup journalAbbreviationsToggleGroup;

    @FXML private RadioButton abbreviateDefault;
    @FXML private RadioButton abbreviateDottles;
    @FXML private RadioButton abbreviateShortestUnique;
    @FXML private RadioButton abbreviateLTWA;
    @FXML private RadioButton unabbreviate;

    private final JournalAbbreviationViewModel viewModel;

    public JournalAbbreviationPanel() {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new JournalAbbreviationViewModel();

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
        //  Initial buttons setup
        journalAbbreviationsToggleGroup.selectToggle(
                journalAbbreviationsToggleGroup.getToggles().stream()
                                               .filter(toggle -> toggle.getUserData().equals(viewModel.selectedJournalCleanupOption.get()))
                                               .findFirst().orElse(null));

        // Listener for user pressing button
        journalAbbreviationsToggleGroup.selectedToggleProperty().addListener((_, _, newToggle) -> {
            if (newToggle != null) {
                CleanupPreferences.CleanupStep step = (CleanupPreferences.CleanupStep) newToggle.getUserData();
                viewModel.selectedJournalCleanupOption.set(step);
            }
        });

        //  Listener for external bindings
        viewModel.selectedJournalCleanupOption.addListener((_, _, newCleanupStep) -> {
            if (newCleanupStep != null) {
                journalAbbreviationsToggleGroup.getToggles().stream()
                                               .filter(toggle -> newCleanupStep.equals(toggle.getUserData()))
                                               .findFirst()
                                               .ifPresent(journalAbbreviationsToggleGroup::selectToggle);
            }
        });
    }

    public ObjectProperty<CleanupPreferences.CleanupStep> selectedJournalCleanupOption() {
        return viewModel.selectedJournalCleanupOption;
    }

    public static EnumSet<CleanupPreferences.CleanupStep> getAllCleanupOptions() {
        return JournalAbbreviationViewModel.CLEANUP_JOURNAL_METHODS;
    }
}
