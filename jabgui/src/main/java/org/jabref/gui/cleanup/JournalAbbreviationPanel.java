package org.jabref.gui.cleanup;

import java.util.EnumSet;

import javafx.beans.property.SetProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import org.jabref.logic.cleanup.CleanupPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class JournalAbbreviationPanel extends VBox {
    @FXML public Label cleanupJournalAbbreviationsLabel;
    @FXML private ToggleGroup journalAbbreviationsToggleGroup;

    @FXML private RadioButton abbreviateDefault;
    @FXML private RadioButton abbreviateDottles;
    @FXML private RadioButton abbreviateShortestUnique;
    @FXML private RadioButton abbreviateLTWA;
    @FXML private RadioButton unabbreviate;
    @FXML private RadioButton noChanges;

    private final JournalAbbreviationViewModel viewModel;

    public JournalAbbreviationPanel() {
        this.viewModel = new JournalAbbreviationViewModel();

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        abbreviateDefault.setUserData(EnumSet.of(CleanupPreferences.CleanupStep.ABBREVIATE_DEFAULT));

        abbreviateDottles.setUserData(EnumSet.of(CleanupPreferences.CleanupStep.ABBREVIATE_DOTLESS));

        abbreviateShortestUnique.setUserData(EnumSet.of(CleanupPreferences.CleanupStep.ABBREVIATE_SHORTEST_UNIQUE));

        abbreviateLTWA.setUserData(EnumSet.of(CleanupPreferences.CleanupStep.ABBREVIATE_LTWA));

        unabbreviate.setUserData(EnumSet.of(CleanupPreferences.CleanupStep.UNABBREVIATE));

        noChanges.setUserData(EnumSet.noneOf(CleanupPreferences.CleanupStep.class));

        bindProperties();
    }

    private void bindProperties() {
        //  Initial buttons setup
        journalAbbreviationsToggleGroup.getToggles().stream()
                                       .filter(toggle -> toggle.getUserData().equals(viewModel.selectedJournalCleanupOption.get()))
                                       .findFirst()
                                       .ifPresent(journalAbbreviationsToggleGroup::selectToggle);

        // Listener for user pressing button
        journalAbbreviationsToggleGroup.selectedToggleProperty().addListener((_, _, newToggle) -> {
            if (newToggle != null) {
                EnumSet<CleanupPreferences.CleanupStep> selectedSteps = (EnumSet<CleanupPreferences.CleanupStep>) newToggle.getUserData();
                viewModel.selectedJournalCleanupOption.set(FXCollections.observableSet(selectedSteps));
            }
        });

        //  Listener for external bindings
        viewModel.selectedJournalCleanupOption.addListener((_, _, newCleanupStep) -> {
            journalAbbreviationsToggleGroup.getToggles().stream()
                                           .filter(toggle -> newCleanupStep.equals(toggle.getUserData()))
                                           .findFirst()
                                           .ifPresent(journalAbbreviationsToggleGroup::selectToggle);
        });
    }

    public SetProperty<CleanupPreferences.CleanupStep> selectedJournalCleanupOption() {
        return viewModel.selectedJournalCleanupOption;
    }

    public static EnumSet<CleanupPreferences.CleanupStep> getAllCleanupOptions() {
        return JournalAbbreviationViewModel.CLEANUP_JOURNAL_METHODS;
    }
}
