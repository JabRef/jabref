package org.jabref.gui.cleanup;

import java.util.EnumSet;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import org.jabref.gui.commonfxcontrols.MultiFieldsCleanupPanel;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupTabSelection;

import com.airhacks.afterburner.views.ViewLoader;
import org.jspecify.annotations.NonNull;

public class CleanupMultiFieldPanel extends VBox implements CleanupPanel {
    @FXML private MultiFieldsCleanupPanel multiFieldsCleanupPanel;

    private final CleanupMultiFieldViewModel viewModel;
    private final CleanupDialogViewModel dialogViewModel;

    public CleanupMultiFieldPanel(@NonNull CleanupPreferences cleanupPreferences,
                                  @NonNull CleanupDialogViewModel dialogViewModel) {

        this.dialogViewModel = dialogViewModel;
        this.viewModel = new CleanupMultiFieldViewModel(cleanupPreferences);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        bindProperties();
    }

    private void bindProperties() {
        multiFieldsCleanupPanel.selectedJobsProperty().bindBidirectional(viewModel.activeJobs);
    }

    @Override
    public CleanupTabSelection getSelectedTab() {
        EnumSet<CleanupPreferences.CleanupStep> activeSteps = EnumSet.noneOf(CleanupPreferences.CleanupStep.class);
        activeSteps.addAll(viewModel.activeJobs.get());

        return CleanupTabSelection.ofJobs(viewModel.allJobs, activeSteps);
    }
}
