package org.jabref.gui.commonfxcontrols;

import java.util.EnumSet;

import javafx.beans.property.SetProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

import org.jabref.logic.cleanup.CleanupPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class MultiFieldsCleanupPanel extends VBox {
    @FXML private CheckBox cleanupDoi;
    @FXML private CheckBox cleanupEprint;
    @FXML private CheckBox cleanupUrl;
    @FXML private CheckBox cleanupBibLaTeX;
    @FXML private CheckBox cleanupBibTeX;
    @FXML private CheckBox cleanupTimestampToCreationDate;
    @FXML private CheckBox cleanupTimestampToModificationDate;

    private final MultiFieldsCleanupViewModel viewModel;

    public MultiFieldsCleanupPanel() {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new MultiFieldsCleanupViewModel();

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

    public EnumSet<CleanupPreferences.CleanupStep> getSelectedJobs() {
        return viewModel.getSelectedJobs();
    }

    public SetProperty<CleanupPreferences.CleanupStep> selectedJobsProperty() {
        return viewModel.selectedJobsProperty();
    }

    public static EnumSet<CleanupPreferences.CleanupStep> getAllJobs() {
        return MultiFieldsCleanupViewModel.MULTI_FIELD_JOBS;
    }
}
