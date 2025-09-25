package org.jabref.gui.cleanup;

import java.util.EnumSet;
import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupTabSelection;

import com.airhacks.afterburner.views.ViewLoader;

public class CleanupMultiFieldPanel extends VBox {
    @FXML private CheckBox cleanupDOI;
    @FXML private CheckBox cleanupEprint;
    @FXML private CheckBox cleanupURL;
    @FXML private CheckBox cleanupBibLaTeX;
    @FXML private CheckBox cleanupBibTeX;
    @FXML private CheckBox cleanupTimestampToCreationDate;
    @FXML private CheckBox cleanupTimestampToModificationDate;

    private final CleanupMultiFieldViewModel viewModel;
    private final CleanupDialogViewModel dialogViewModel;

    public CleanupMultiFieldPanel(CleanupPreferences cleanupPreferences,
                                  CleanupDialogViewModel dialogViewModel) {
        Objects.requireNonNull(cleanupPreferences, "cleanupPreferences must not be null");
        Objects.requireNonNull(dialogViewModel, "viewModel must not be null");

        this.dialogViewModel = dialogViewModel;
        this.viewModel = new CleanupMultiFieldViewModel(cleanupPreferences);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        bindProperties();
    }

    private void bindProperties() {
        cleanupDOI.selectedProperty().bindBidirectional(viewModel.doiSelected);
        cleanupEprint.selectedProperty().bindBidirectional(viewModel.eprintSelected);
        cleanupURL.selectedProperty().bindBidirectional(viewModel.urlSelected);
        cleanupBibTeX.selectedProperty().bindBidirectional(viewModel.bibTexSelected);
        cleanupBibLaTeX.selectedProperty().bindBidirectional(viewModel.bibLaTexSelected);
        cleanupTimestampToCreationDate.selectedProperty().bindBidirectional(viewModel.timestampToCreationSelected);
        cleanupTimestampToModificationDate.selectedProperty().bindBidirectional(viewModel.timestampToModificationSelected);
    }

    @FXML
    private void onApply() {
        EnumSet<CleanupPreferences.CleanupStep> selectedJobs = viewModel.getSelectedJobs();
        CleanupTabSelection selectedTab = CleanupTabSelection.ofJobs(CleanupDialogViewModel.MULTI_FIELD_JOBS, selectedJobs);
        dialogViewModel.apply(selectedTab);
        getScene().getWindow().hide();
    }
}
