package org.jabref.gui.commonfxcontrols;

import java.util.EnumSet;

import javafx.beans.property.SetProperty;
import javafx.collections.SetChangeListener;
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
        this.viewModel = new MultiFieldsCleanupViewModel();

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        cleanupDoi.setOnAction(_ -> viewModel.toggleStep(CleanupPreferences.CleanupStep.CLEAN_UP_DOI, cleanupDoi.isSelected()));
        cleanupEprint.setOnAction(_ -> viewModel.toggleStep(CleanupPreferences.CleanupStep.CLEANUP_EPRINT, cleanupEprint.isSelected()));
        cleanupUrl.setOnAction(_ -> viewModel.toggleStep(CleanupPreferences.CleanupStep.CLEAN_UP_URL, cleanupUrl.isSelected()));
        cleanupBibTeX.setOnAction(_ -> viewModel.toggleStep(CleanupPreferences.CleanupStep.CONVERT_TO_BIBTEX, cleanupBibTeX.isSelected()));
        cleanupBibLaTeX.setOnAction(_ -> viewModel.toggleStep(CleanupPreferences.CleanupStep.CONVERT_TO_BIBLATEX, cleanupBibLaTeX.isSelected()));
        cleanupTimestampToCreationDate.setOnAction(_ -> viewModel.toggleStep(CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_CREATIONDATE, cleanupTimestampToCreationDate.isSelected()));
        cleanupTimestampToModificationDate.setOnAction(_ -> viewModel.toggleStep(CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_MODIFICATIONDATE, cleanupTimestampToModificationDate.isSelected()));

        viewModel.selectedJobsProperty().addListener((SetChangeListener<CleanupPreferences.CleanupStep>) change -> {
            if (change.wasAdded()) {
                setCheckBoxSelected(change.getElementAdded(), true);
            }
            if (change.wasRemoved()) {
                setCheckBoxSelected(change.getElementRemoved(), false);
            }
        });
    }

    private void setCheckBoxSelected(CleanupPreferences.CleanupStep step, boolean isSelected) {
        switch (step) {
            case CLEAN_UP_DOI ->
                    cleanupDoi.setSelected(isSelected);
            case CLEANUP_EPRINT ->
                    cleanupEprint.setSelected(isSelected);
            case CLEAN_UP_URL ->
                    cleanupUrl.setSelected(isSelected);
            case CONVERT_TO_BIBTEX ->
                    cleanupBibTeX.setSelected(isSelected);
            case CONVERT_TO_BIBLATEX ->
                    cleanupBibLaTeX.setSelected(isSelected);
            case CONVERT_TIMESTAMP_TO_CREATIONDATE ->
                    cleanupTimestampToCreationDate.setSelected(isSelected);
            case CONVERT_TIMESTAMP_TO_MODIFICATIONDATE ->
                    cleanupTimestampToModificationDate.setSelected(isSelected);
            default -> {
                //  Ignore
            }
        }
    }

    public SetProperty<CleanupPreferences.CleanupStep> selectedJobsProperty() {
        return viewModel.selectedJobsProperty();
    }

    public static EnumSet<CleanupPreferences.CleanupStep> getAllJobs() {
        return MultiFieldsCleanupViewModel.MULTI_FIELD_JOBS;
    }
}
