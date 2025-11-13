package org.jabref.gui.cleanup;

import java.util.EnumSet;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupTabSelection;

import com.airhacks.afterburner.views.ViewLoader;
import org.jspecify.annotations.NonNull;

public class CleanupMultiFieldPanel extends VBox {
    @FXML private CheckBox cleanupDoi;
    @FXML private CheckBox cleanupEprint;
    @FXML private CheckBox cleanupUrl;
    @FXML private CheckBox cleanupBibLaTeX;
    @FXML private CheckBox cleanupBibTeX;
    @FXML private CheckBox cleanupTimestampToCreationDate;
    @FXML private CheckBox cleanupTimestampToModificationDate;

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
        cleanupDoi.selectedProperty().bindBidirectional(viewModel.doiSelected);
        cleanupEprint.selectedProperty().bindBidirectional(viewModel.eprintSelected);
        cleanupUrl.selectedProperty().bindBidirectional(viewModel.urlSelected);
        cleanupBibTeX.selectedProperty().bindBidirectional(viewModel.bibTexSelected);
        cleanupBibLaTeX.selectedProperty().bindBidirectional(viewModel.bibLaTexSelected);
        cleanupTimestampToCreationDate.selectedProperty().bindBidirectional(viewModel.timestampToCreationSelected);
        cleanupTimestampToModificationDate.selectedProperty().bindBidirectional(viewModel.timestampToModificationSelected);
    }

    @FXML
    private void onApply() {
        EnumSet<CleanupPreferences.CleanupStep> selectedJobs = viewModel.getSelectedJobs();
        CleanupTabSelection selectedTab = CleanupTabSelection.ofJobs(CleanupMultiFieldViewModel.MULTI_FIELD_JOBS, selectedJobs);
        dialogViewModel.apply(selectedTab);
        getScene().getWindow().hide();
    }
}
