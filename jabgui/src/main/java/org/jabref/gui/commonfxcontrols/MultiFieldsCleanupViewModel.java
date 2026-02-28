package org.jabref.gui.commonfxcontrols;

import java.util.EnumSet;

import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.cleanup.CleanupPreferences;

public class MultiFieldsCleanupViewModel {
    public static final EnumSet<CleanupPreferences.CleanupStep> MULTI_FIELD_JOBS = EnumSet.of(
            CleanupPreferences.CleanupStep.CLEAN_UP_DOI,
            CleanupPreferences.CleanupStep.CLEANUP_EPRINT,
            CleanupPreferences.CleanupStep.CLEAN_UP_URL,
            CleanupPreferences.CleanupStep.CONVERT_TO_BIBLATEX,
            CleanupPreferences.CleanupStep.CONVERT_TO_BIBTEX,
            CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_CREATIONDATE,
            CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_MODIFICATIONDATE
    );

    private final SetProperty<CleanupPreferences.CleanupStep> selectedJobs = new SimpleSetProperty<>(FXCollections.observableSet());

    public MultiFieldsCleanupViewModel() {
    }

    public void toggleStep(CleanupPreferences.CleanupStep step, boolean isSelected) {
        if (isSelected) {
            mutuallyExclusiveSteps(step);
            selectedJobs.add(step);
        } else {
            selectedJobs.remove(step);
        }
    }

    private void mutuallyExclusiveSteps(CleanupPreferences.CleanupStep addedStep) {
        switch (addedStep) {
            case CONVERT_TO_BIBTEX ->
                    selectedJobs.remove(CleanupPreferences.CleanupStep.CONVERT_TO_BIBLATEX);
            case CONVERT_TO_BIBLATEX ->
                    selectedJobs.remove(CleanupPreferences.CleanupStep.CONVERT_TO_BIBTEX);
            case CONVERT_TIMESTAMP_TO_CREATIONDATE ->
                    selectedJobs.remove(CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_MODIFICATIONDATE);
            case CONVERT_TIMESTAMP_TO_MODIFICATIONDATE ->
                    selectedJobs.remove(CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_CREATIONDATE);
            default -> {
                //  Do nothing for other steps
            }
        }
    }

    public SetProperty<CleanupPreferences.CleanupStep> selectedJobsProperty() {
        return selectedJobs;
    }
}
