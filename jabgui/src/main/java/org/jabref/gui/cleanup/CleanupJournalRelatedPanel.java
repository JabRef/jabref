package org.jabref.gui.cleanup;

import java.util.EnumSet;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupTabSelection;

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
                            "Abbreviate (default)";
                    case ABBREVIATE_DOTLESS ->
                            "Abbreviate (dotless)";
                    case ABBREVIATE_SHORTEST_UNIQUE ->
                            "Abbreviate (shortest unique)";
                    case UNABBREVIATE ->
                            "Unabbreviate";
                    case NO_CHANGES ->
                            "No changes";
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
        cleanupJournalBox.valueProperty().bindBidirectional(viewModel.selectedJournalCleanupOptionProperty());
    }

    @Override
    public CleanupTabSelection getSelectedTab() {
        EnumSet<CleanupPreferences.CleanupStep> selectedJobs = EnumSet.noneOf(CleanupPreferences.CleanupStep.class);
        selectedJobs.add(viewModel.getSelectedJournalCleanupOption());

        return CleanupTabSelection.ofJobs(CleanupJournalRelatedViewModel.CLEANUP_JOURNAL_METHODS, selectedJobs);
    }
}
