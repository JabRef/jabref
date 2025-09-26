package org.jabref.gui.cleanup;

import java.util.EnumSet;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.logic.cleanup.CleanupPreferences;

public class CleanupMultiFieldViewModel {

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

    public CleanupMultiFieldViewModel(CleanupPreferences preferences) {
        doiSelected.set(preferences.isActive(CleanupPreferences.CleanupStep.CLEAN_UP_DOI));
        eprintSelected.set(preferences.isActive(CleanupPreferences.CleanupStep.CLEANUP_EPRINT));
        urlSelected.set(preferences.isActive(CleanupPreferences.CleanupStep.CLEAN_UP_URL));
        bibTexSelected.set(preferences.isActive(CleanupPreferences.CleanupStep.CONVERT_TO_BIBTEX));
        bibLaTexSelected.set(preferences.isActive(CleanupPreferences.CleanupStep.CONVERT_TO_BIBLATEX));
        timestampToCreationSelected.set(preferences.isActive(CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_CREATIONDATE));
        timestampToModificationSelected.set(preferences.isActive(CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_MODIFICATIONDATE));

        bibTexSelected.addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal) {
                        bibLaTexSelected.set(false);
                    }
                }
        );
        bibLaTexSelected.addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal) {
                        bibTexSelected.set(false);
                    }
                }
        );
        timestampToCreationSelected.addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal) {
                        timestampToModificationSelected.set(false);
                    }
                }
        );
        timestampToModificationSelected.addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal) {
                        timestampToCreationSelected.set(false);
                    }
                }
        );
    }

    public EnumSet<CleanupPreferences.CleanupStep> getSelectedJobs() {
        EnumSet<CleanupPreferences.CleanupStep> activeJobs = EnumSet.noneOf(CleanupPreferences.CleanupStep.class);
        if (doiSelected.get()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CLEAN_UP_DOI);
        }
        if (eprintSelected.get()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CLEANUP_EPRINT);
        }
        if (urlSelected.get()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CLEAN_UP_URL);
        }
        if (bibTexSelected.get()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CONVERT_TO_BIBTEX);
        }
        if (bibLaTexSelected.get()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CONVERT_TO_BIBLATEX);
        }
        if (timestampToCreationSelected.get()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_CREATIONDATE);
        }
        if (timestampToModificationSelected.get()) {
            activeJobs.add(CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_MODIFICATIONDATE);
        }
        return activeJobs;
    }
}
