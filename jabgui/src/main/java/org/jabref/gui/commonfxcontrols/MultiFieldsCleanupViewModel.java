package org.jabref.gui.commonfxcontrols;

import java.util.EnumSet;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.SetChangeListener;

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

    public final BooleanProperty doiSelected = new SimpleBooleanProperty();
    public final BooleanProperty eprintSelected = new SimpleBooleanProperty();
    public final BooleanProperty urlSelected = new SimpleBooleanProperty();
    public final BooleanProperty bibTexSelected = new SimpleBooleanProperty();
    public final BooleanProperty bibLaTexSelected = new SimpleBooleanProperty();
    public final BooleanProperty timestampToCreationSelected = new SimpleBooleanProperty();
    public final BooleanProperty timestampToModificationSelected = new SimpleBooleanProperty();

    private final SetProperty<CleanupPreferences.CleanupStep> selectedJobs = new SimpleSetProperty<>(FXCollections.observableSet());

    private boolean updating = false;

    public MultiFieldsCleanupViewModel() {
        bibTexSelected.addListener((_, _, newVal) -> {
            if (newVal) {
                bibLaTexSelected.set(false);
            }
        });
        bibLaTexSelected.addListener((_, _, newVal) -> {
            if (newVal) {
                bibTexSelected.set(false);
            }
        });
        timestampToCreationSelected.addListener((_, _, newVal) -> {
            if (newVal) {
                timestampToModificationSelected.set(false);
            }
        });
        timestampToModificationSelected.addListener((_, _, newVal) -> {
            if (newVal) {
                timestampToCreationSelected.set(false);
            }
        });

        addStepListener(doiSelected, CleanupPreferences.CleanupStep.CLEAN_UP_DOI);
        addStepListener(eprintSelected, CleanupPreferences.CleanupStep.CLEANUP_EPRINT);
        addStepListener(urlSelected, CleanupPreferences.CleanupStep.CLEAN_UP_URL);
        addStepListener(bibTexSelected, CleanupPreferences.CleanupStep.CONVERT_TO_BIBTEX);
        addStepListener(bibLaTexSelected, CleanupPreferences.CleanupStep.CONVERT_TO_BIBLATEX);
        addStepListener(timestampToCreationSelected, CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_CREATIONDATE);
        addStepListener(timestampToModificationSelected, CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_MODIFICATIONDATE);

        selectedJobs.addListener((SetChangeListener<CleanupPreferences.CleanupStep>) change -> {
            if (updating) {
                return;
            }

            try {
                updating = true;
                if (change.wasAdded()) {
                    CleanupPreferences.CleanupStep addedStep = change.getElementAdded();
                    if (!MULTI_FIELD_JOBS.contains(addedStep)) {
                        throw new UnsupportedOperationException(addedStep + ": is unsupported by multi field jobs");
                    }
                    setBooleanPropertyForStep(addedStep, true);
                }
                if (change.wasRemoved()) {
                    setBooleanPropertyForStep(change.getElementRemoved(), false);
                }
            } finally {
                updating = false;
            }
        });
    }

    private void addStepListener(BooleanProperty property, CleanupPreferences.CleanupStep step) {
        property.addListener((_, _, newVal) -> {
            if (updating) {
                return;
            }

            try {
                updating = true;
                if (newVal) {
                    selectedJobs.add(step);
                } else {
                    selectedJobs.remove(step);
                }
            } finally {
                updating = false;
            }
        });
    }

    private void setBooleanPropertyForStep(CleanupPreferences.CleanupStep step, boolean isSelected) {
        switch (step) {
            case CLEAN_UP_DOI ->
                    doiSelected.set(isSelected);
            case CLEANUP_EPRINT ->
                    eprintSelected.set(isSelected);
            case CLEAN_UP_URL ->
                    urlSelected.set(isSelected);
            case CONVERT_TO_BIBTEX ->
                    bibTexSelected.set(isSelected);
            case CONVERT_TO_BIBLATEX ->
                    bibLaTexSelected.set(isSelected);
            case CONVERT_TIMESTAMP_TO_CREATIONDATE ->
                    timestampToCreationSelected.set(isSelected);
            case CONVERT_TIMESTAMP_TO_MODIFICATIONDATE ->
                    timestampToModificationSelected.set(isSelected);
            default -> { /* Ignore steps that are not managed by checkboxes */ }
        }
    }

    public SetProperty<CleanupPreferences.CleanupStep> selectedJobsProperty() {
        return selectedJobs;
    }
}
