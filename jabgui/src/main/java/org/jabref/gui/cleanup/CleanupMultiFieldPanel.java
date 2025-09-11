package org.jabref.gui.cleanup;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

import org.jabref.logic.cleanup.CleanupPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class CleanupMultiFieldPanel extends VBox implements CleanupPanel {
    @FXML private CheckBox cleanupDOI;
    @FXML private CheckBox cleanupEprint;
    @FXML private CheckBox cleanupURL;
    @FXML private CheckBox cleanupBiblatex;
    @FXML private CheckBox cleanupBibtex;
    @FXML private CheckBox cleanupTimestampToCreationDate;
    @FXML private CheckBox cleanupTimestampToModificationDate;

    public CleanupMultiFieldPanel(CleanupPreferences cleanupPreferences) {
        Objects.requireNonNull(cleanupPreferences, "cleanupPreferences must not be null");

        ViewLoader.view(this)
                  .root(this)
                  .load();

        init(cleanupPreferences);
    }

    private void init(CleanupPreferences cleanupPreferences) {
        cleanupBibtex.selectedProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue) {
                        cleanupBiblatex.selectedProperty().setValue(false);
                    }
                });
        cleanupBiblatex.selectedProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue) {
                        cleanupBibtex.selectedProperty().setValue(false);
                    }
                });

        cleanupTimestampToCreationDate.selectedProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue) {
                        cleanupTimestampToModificationDate.selectedProperty().setValue(false);
                    }
                });
        cleanupTimestampToModificationDate.selectedProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue) {
                        cleanupTimestampToCreationDate.selectedProperty().setValue(false);
                    }
                });
        updateDisplay(cleanupPreferences);
    }

    private void updateDisplay(CleanupPreferences preset) {
        cleanupDOI.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CLEAN_UP_DOI));
        cleanupEprint.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CLEANUP_EPRINT));
        cleanupURL.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CLEAN_UP_URL));
        cleanupBiblatex.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CONVERT_TO_BIBLATEX));
        cleanupBibtex.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CONVERT_TO_BIBTEX));
        cleanupTimestampToCreationDate.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_CREATIONDATE));
        cleanupTimestampToModificationDate.setSelected(preset.isActive(CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_MODIFICATIONDATE));
        cleanupTimestampToModificationDate.setSelected(preset.isActive(CleanupPreferences.CleanupStep.DO_NOT_CONVERT_TIMESTAMP));
    }

    @Override
    public Optional<CleanupPreferences> getCleanupPreferences() {
        EnumSet<CleanupPreferences.CleanupStep> activeJobs = EnumSet.noneOf(CleanupPreferences.CleanupStep.class);

        if (cleanupDOI.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CLEAN_UP_DOI);
        }
        if (cleanupEprint.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CLEANUP_EPRINT);
        }
        if (cleanupURL.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CLEAN_UP_URL);
        }
        if (cleanupBiblatex.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CONVERT_TO_BIBLATEX);
        }
        if (cleanupBibtex.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CONVERT_TO_BIBTEX);
        }
        if (cleanupTimestampToCreationDate.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_CREATIONDATE);
        }
        if (cleanupTimestampToModificationDate.isSelected()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_MODIFICATIONDATE);
        }

        return Optional.of(new CleanupPreferences(activeJobs));
    }
}
