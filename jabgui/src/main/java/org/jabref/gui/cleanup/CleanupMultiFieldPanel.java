package org.jabref.gui.cleanup;

import java.util.EnumSet;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

import org.jabref.logic.cleanup.CleanupPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class CleanupMultiFieldPanel extends VBox implements CleanupPanel {
    @FXML private CheckBox cleanUpDOI;
    @FXML private CheckBox cleanUpEprint;
    @FXML private CheckBox cleanUpURL;
    @FXML private CheckBox cleanUpBiblatex;
    @FXML private CheckBox cleanUpBibtex;
    @FXML private CheckBox cleanUpTimestampToCreationDate;
    @FXML private CheckBox cleanUpTimestampToModificationDate;

    public CleanupMultiFieldPanel(CleanupPreferences cleanupPreferences) {
        // Load FXML
        ViewLoader.view(this)
                  .root(this)
                  .load();

        init(cleanupPreferences);
    }

    private void init(CleanupPreferences cleanupPreferences) {
        cleanUpBibtex.selectedProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue) {
                        cleanUpBiblatex.selectedProperty().setValue(false);
                    }
                });
        cleanUpBiblatex.selectedProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue) {
                        cleanUpBibtex.selectedProperty().setValue(false);
                    }
                });

        cleanUpTimestampToCreationDate.selectedProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue) {
                        cleanUpTimestampToModificationDate.selectedProperty().setValue(false);
                    }
                });
        cleanUpTimestampToModificationDate.selectedProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue) {
                        cleanUpTimestampToCreationDate.selectedProperty().setValue(false);
                    }
                });
        updateDisplay(cleanupPreferences);
    }

    private void updateDisplay(CleanupPreferences preset) {
        cleanUpDOI.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CLEAN_UP_DOI));
        cleanUpEprint.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CLEANUP_EPRINT));
        cleanUpURL.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CLEAN_UP_URL));
        cleanUpBiblatex.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CONVERT_TO_BIBLATEX));
        cleanUpBibtex.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CONVERT_TO_BIBTEX));
        cleanUpTimestampToCreationDate.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_CREATIONDATE));
        cleanUpTimestampToModificationDate.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_MODIFICATIONDATE));
        cleanUpTimestampToModificationDate.setSelected(preset.isActive(CleanupPreferences.CleanupStep.DO_NOT_CONVERT_TIMESTAMP));
    }

    @Override
    public CleanupPreferences getCleanupPreferences() {
        EnumSet<CleanupPreferences.CleanupStep> activeJobs = EnumSet.noneOf(CleanupPreferences.CleanupStep.class);

        if (cleanUpDOI.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CLEAN_UP_DOI);
        }
        if (cleanUpEprint.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CLEANUP_EPRINT);
        }
        if (cleanUpURL.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CLEAN_UP_URL);
        }
        if (cleanUpBiblatex.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CONVERT_TO_BIBLATEX);
        }
        if (cleanUpBibtex.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CONVERT_TO_BIBTEX);
        }
        if (cleanUpTimestampToCreationDate.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_CREATIONDATE);
        }
        if (cleanUpTimestampToModificationDate.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_MODIFICATIONDATE);
        }

        return new CleanupPreferences(activeJobs);
    }
}
