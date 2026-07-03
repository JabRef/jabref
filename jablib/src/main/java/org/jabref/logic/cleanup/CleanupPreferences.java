package org.jabref.logic.cleanup;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

public class CleanupPreferences {

    private static final EnumSet<CleanupStep> DEFAULT_ACTIVE_JOBS = EnumSet.of(
            CleanupStep.CLEAN_UP_DOI,
            CleanupStep.CLEAN_UP_ARXIV_DOI,
            CleanupStep.CLEANUP_EPRINT,
            CleanupStep.CLEAN_UP_URL,
            CleanupStep.MAKE_PATHS_RELATIVE,
            CleanupStep.RENAME_PDF,
            CleanupStep.FIX_FILE_LINKS,
            CleanupStep.CLEAN_UP_DELETED_LINKED_FILES,
            CleanupStep.REMOVE_XMP_METADATA,
            CleanupStep.CONVERT_TIMESTAMP_TO_CREATIONDATE,
            CleanupStep.CONVERT_TIMESTAMP_TO_MODIFICATIONDATE,
            CleanupStep.DO_NOT_CONVERT_TIMESTAMP);

    private static final FieldFormatterCleanupActions DEFAULT_FIELD_FORMATTER_CLEANUPS =
            new FieldFormatterCleanupActions(false, FieldFormatterCleanupActions.DEFAULT_SAVE_ACTIONS);

    private final ObservableSet<CleanupStep> activeJobs;
    private final ObjectProperty<FieldFormatterCleanupActions> fieldFormatterCleanups;

    private CleanupPreferences() {
        this(
                EnumSet.copyOf(DEFAULT_ACTIVE_JOBS),  // copy to prevent mutation of static field
                DEFAULT_FIELD_FORMATTER_CLEANUPS      // Default field formatter cleanups (disabled)
        );
    }

    public CleanupPreferences(Set<CleanupStep> activeJobs) {
        this(
                EnumSet.copyOf(activeJobs),
                new FieldFormatterCleanupActions(false, FieldFormatterCleanupActions.DEFAULT_SAVE_ACTIONS));
    }

    public CleanupPreferences(CleanupStep activeJob) {
        this(EnumSet.of(activeJob));
    }

    public CleanupPreferences(FieldFormatterCleanupActions formatterCleanups) {
        this(EnumSet.noneOf(CleanupStep.class), formatterCleanups);
    }

    public CleanupPreferences(Set<CleanupStep> activeJobs, FieldFormatterCleanupActions formatterCleanups) {
        this.activeJobs = FXCollections.observableSet(new HashSet<>(activeJobs));
        this.fieldFormatterCleanups = new SimpleObjectProperty<>(formatterCleanups);
    }

    public static CleanupPreferences getDefault() {
        return new CleanupPreferences();
    }

    public Set<CleanupStep> getActiveJobs() {
        return activeJobs;
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
        // Removes the http://... for each DOI. Moves DOIs from URL and NOTE filed to DOI field.
        CLEAN_UP_DOI,

        // Consolidates the arXiv identifier onto the (version-stable) DOI field and drops the redundant eprint.
        CLEAN_UP_ARXIV_DOI,

        CLEANUP_EPRINT,
        CLEAN_UP_URL,

        MAKE_PATHS_RELATIVE,
        RENAME_PDF,
        RENAME_PDF_ONLY_RELATIVE_PATHS,
        RENAME_PDF_ONLY_PDF_FILES,
        MOVE_PDF,
        FIX_FILE_LINKS,
        CLEAN_UP_DELETED_LINKED_FILES,
        REMOVE_XMP_METADATA,

        // Collects file links from the pdf or ps field, and adds them to the list contained in the file field.
        CLEAN_UP_UPGRADE_EXTERNAL_LINKS,

        CONVERT_TO_BIBLATEX,
        CONVERT_TO_BIBTEX,

        CONVERT_TIMESTAMP_TO_CREATIONDATE,
        CONVERT_TIMESTAMP_TO_MODIFICATIONDATE,
        DO_NOT_CONVERT_TIMESTAMP,

        // Converts Math Subject Classification Codes presented in Keywords into their Descriptions
        CONVERT_MSC_CODES,

        // Abbreviate or unabbreviate journal titles
        ABBREVIATE_DEFAULT,
        ABBREVIATE_DOTLESS,
        ABBREVIATE_SHORTEST_UNIQUE,
        ABBREVIATE_LTWA,
        UNABBREVIATE;

        /// Parses a step name, returning an empty Optional for unknown or removed values.
        ///
        /// Used when loading preferences written by older versions: e.g. the former `CLEAN_UP_ISSN`
        /// step was removed (ISSN normalization now happens via the `NormalizeIssn` field formatter),
        /// and such a stored value must not break preference loading.
        public static Optional<CleanupStep> safeValueOf(String name) {
            try {
                return Optional.of(valueOf(name));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
    }
}
