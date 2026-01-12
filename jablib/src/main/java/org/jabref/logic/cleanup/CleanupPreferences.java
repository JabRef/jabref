package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

public class CleanupPreferences {
    private final ObservableSet<CleanupStep> activeJobs;
    private final ObjectProperty<FieldFormatterCleanupActions> fieldFormatterCleanups;

    public CleanupPreferences(EnumSet<CleanupStep> activeJobs) {
        this(activeJobs, new FieldFormatterCleanupActions(false, new ArrayList<>()));
    }

    public CleanupPreferences(CleanupStep activeJob) {
        this(EnumSet.of(activeJob));
    }

    public CleanupPreferences(FieldFormatterCleanupActions formatterCleanups) {
        this(EnumSet.noneOf(CleanupStep.class), formatterCleanups);
    }

    public CleanupPreferences(EnumSet<CleanupStep> activeJobs, FieldFormatterCleanupActions formatterCleanups) {
        this.activeJobs = FXCollections.observableSet(activeJobs);
        this.fieldFormatterCleanups = new SimpleObjectProperty<>(formatterCleanups);
    }

    public EnumSet<CleanupStep> getActiveJobs() {
        if (activeJobs.isEmpty()) {
            return EnumSet.noneOf(CleanupStep.class);
        }

        return EnumSet.copyOf(activeJobs);
    }

    public void setActive(CleanupStep job, boolean value) {
        if (activeJobs.contains(job) && !value) {
            activeJobs.remove(job);
        } else if (!activeJobs.contains(job) && value) {
            activeJobs.add(job);
        }
    }

    public ObservableSet<CleanupStep> getObservableActiveJobs() {
        return activeJobs;
    }

    public void setActiveJobs(Set<CleanupStep> jobs) {
        activeJobs.clear();
        activeJobs.addAll(jobs);
    }

    public Boolean isActive(CleanupStep step) {
        return activeJobs.contains(step);
    }

    public FieldFormatterCleanupActions getFieldFormatterCleanups() {
        return fieldFormatterCleanups.get();
    }

    public ObjectProperty<FieldFormatterCleanupActions> fieldFormatterCleanupsProperty() {
        return fieldFormatterCleanups;
    }

    public void setFieldFormatterCleanups(FieldFormatterCleanupActions fieldFormatters) {
        fieldFormatterCleanups.setValue(fieldFormatters);
    }

    public enum CleanupStep {
        /**
         * Removes the http://... for each DOI. Moves DOIs from URL and NOTE filed to DOI field.
         */
        CLEAN_UP_DOI,
        CLEANUP_EPRINT,
        CLEAN_UP_URL,
        MAKE_PATHS_RELATIVE,
        RENAME_PDF,
        RENAME_PDF_ONLY_RELATIVE_PATHS,
        /**
         * Collects file links from the pdf or ps field, and adds them to the list contained in the file field.
         */
        CLEAN_UP_UPGRADE_EXTERNAL_LINKS,
        CLEAN_UP_DELETED_LINKED_FILES,
        /**
         * Converts to biblatex format
         */
        CONVERT_TO_BIBLATEX,
        /**
         * Converts to bibtex format
         */
        CONVERT_TO_BIBTEX,
        CONVERT_TIMESTAMP_TO_CREATIONDATE,
        CONVERT_TIMESTAMP_TO_MODIFICATIONDATE,
        DO_NOT_CONVERT_TIMESTAMP,
        MOVE_PDF,
        FIX_FILE_LINKS,
        CLEAN_UP_ISSN,
        /*
         * Converts Math Subject Classification Codes presented in Keywords into their Descriptions
         */
        CONVERT_MSC_CODES,
        /**
         * Abbreviate or unabbreviate journal titles
         */
        ABBREVIATE_DEFAULT,
        ABBREVIATE_DOTLESS,
        ABBREVIATE_SHORTEST_UNIQUE,
        UNABBREVIATE,
        ABBREVIATION_NO_CHANGES
    }
}
