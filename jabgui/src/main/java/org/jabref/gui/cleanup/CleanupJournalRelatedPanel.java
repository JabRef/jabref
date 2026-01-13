package org.jabref.gui.cleanup;

import java.util.EnumSet;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupTabSelection;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import org.jspecify.annotations.NonNull;

public class CleanupJournalRelatedPanel extends VBox implements CleanupPanel {
    @FXML private ComboBox<CleanupPreferences.CleanupStep> cleanupJournalBox;

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
        cleanupJournalBox.getItems().setAll(CleanupJournalRelatedViewModel.CLEANUP_JOURNAL_METHODS);

        cleanupJournalBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(CleanupPreferences.CleanupStep object) {
                return switch (object) {
                    case null ->
                            "";
                    case ABBREVIATE_DEFAULT ->
                            Localization.lang("Abbreviate (default)");
                    case ABBREVIATE_DOTLESS ->
                            Localization.lang("Abbreviate (dotless)");
                    case ABBREVIATE_SHORTEST_UNIQUE ->
                            Localization.lang("Abbreviate (shortest unique)");
                    case UNABBREVIATE ->
                            Localization.lang("Unabbreviate");
                    case ABBREVIATION_NO_CHANGES ->
                            Localization.lang("No changes");
                    default ->
                            object.toString();
                };
            }

            @Override
            public CleanupPreferences.CleanupStep fromString(String string) {
                // Not needed for a non-editable ComboBox
                return null;
            }
        });
    }

    private void bindProperties() {
        cleanupJournalBox.valueProperty().bindBidirectional(viewModel.selectedJournalCleanupOption);
    }

    @Override
    public CleanupTabSelection getSelectedTab() {
        EnumSet<CleanupPreferences.CleanupStep> selectedMethods = EnumSet.noneOf(CleanupPreferences.CleanupStep.class);

        CleanupPreferences.CleanupStep selected = viewModel.selectedJournalCleanupOption.get();
        if (!selected.equals(CleanupPreferences.CleanupStep.ABBREVIATION_NO_CHANGES)) {
            selectedMethods.add(selected);
        }

        return CleanupTabSelection.ofJobs(CleanupJournalRelatedViewModel.CLEANUP_JOURNAL_METHODS, selectedMethods);
    }
}
