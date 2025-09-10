package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

public class CleanupPreferences {

    private final ObservableSet<CleanupStep> activeJobs;
    private final ObjectProperty<FieldFormatterCleanups> fieldFormatterCleanups;

    public CleanupPreferences(EnumSet<CleanupStep> activeJobs) {
        this(activeJobs, new FieldFormatterCleanups(false, new ArrayList<>()));
    }

    public CleanupPreferences(CleanupStep activeJob) {
        this(EnumSet.of(activeJob));
    }

    public CleanupPreferences(FieldFormatterCleanups formatterCleanups) {
        this(EnumSet.noneOf(CleanupStep.class), formatterCleanups);
    }

    public CleanupPreferences(EnumSet<CleanupStep> activeJobs, FieldFormatterCleanups formatterCleanups) {
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

    public FieldFormatterCleanups getFieldFormatterCleanups() {
        return fieldFormatterCleanups.get();
    }

    public ObjectProperty<FieldFormatterCleanups> fieldFormatterCleanupsProperty() {
        return fieldFormatterCleanups;
    }

    public void setFieldFormatterCleanups(FieldFormatterCleanups fieldFormatters) {
        fieldFormatterCleanups.setValue(fieldFormatters);
    }

    /*
     * Categories are used to group CleanupSteps by tab type. This allows the updateWith()
     * method to replace only the steps of the same category when merging preferences from
     * a single tab, without affecting other categories.
     */
    public CleanupPreferences updateWith(CleanupPreferences tabPreferences) {
        EnumSet<CleanupStep> mergedJobs = getActiveJobs();

        Optional<CleanupStepCategory> updatedCategory =
                tabPreferences.getActiveJobs().stream()
                              .map(CleanupStep::getCategory)
                              .findFirst();

        updatedCategory.ifPresent(category -> {
                mergedJobs.removeIf(step -> step.getCategory() == category);
                mergedJobs.addAll(tabPreferences.getActiveJobs());
            }
        );

        return new CleanupPreferences(
                mergedJobs,
                tabPreferences.getFieldFormatterCleanups() != null
                        ? tabPreferences.getFieldFormatterCleanups()
                        : getFieldFormatterCleanups()
        );
    }

    public enum CleanupStep {
        /**
         * Removes the http://... for each DOI. Moves DOIs from URL and NOTE filed to DOI field.
         */
        CLEAN_UP_DOI(CleanupStepCategory.MULTI_FIELD),
        CLEANUP_EPRINT(CleanupStepCategory.MULTI_FIELD),
        CLEAN_UP_URL(CleanupStepCategory.MULTI_FIELD),
        /**
         * Converts to biblatex format
         */
        CONVERT_TO_BIBLATEX(CleanupStepCategory.MULTI_FIELD),
        /**
         * Converts to bibtex format
         */
        CONVERT_TO_BIBTEX(CleanupStepCategory.MULTI_FIELD),
        CONVERT_TIMESTAMP_TO_CREATIONDATE(CleanupStepCategory.MULTI_FIELD),
        CONVERT_TIMESTAMP_TO_MODIFICATIONDATE(CleanupStepCategory.MULTI_FIELD),
        DO_NOT_CONVERT_TIMESTAMP(CleanupStepCategory.MULTI_FIELD),

        MOVE_PDF(CleanupStepCategory.FILE_RELATED),
        MAKE_PATHS_RELATIVE(CleanupStepCategory.FILE_RELATED),
        RENAME_PDF(CleanupStepCategory.FILE_RELATED),
        RENAME_PDF_ONLY_RELATIVE_PATHS(CleanupStepCategory.FILE_RELATED),
        /**
         * Collects file links from the pdf or ps field, and adds them to the list contained in the file field.
         */
        CLEAN_UP_UPGRADE_EXTERNAL_LINKS(CleanupStepCategory.FILE_RELATED),
        CLEAN_UP_DELETED_LINKED_FILES(CleanupStepCategory.FILE_RELATED),

        FIX_FILE_LINKS(CleanupStepCategory.NONE),
        CLEAN_UP_ISSN(CleanupStepCategory.NONE),
        /*
         * Converts Math Subject Classification Codes presented in Keywords into their Descriptions
         */
        CONVERT_MSC_CODES(CleanupStepCategory.NONE);

        private final CleanupStepCategory category;

        CleanupStep(CleanupStepCategory category) {
            this.category = category;
        }

        public CleanupStepCategory getCategory() {
            return category;
        }
    }

    public enum CleanupStepCategory {
        MULTI_FIELD,
        FILE_RELATED,
        NONE
    }
}
