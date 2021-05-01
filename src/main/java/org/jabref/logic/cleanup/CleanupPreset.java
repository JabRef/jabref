package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public class CleanupPreset {

    private final Set<CleanupStep> activeJobs;
    private final FieldFormatterCleanups formatterCleanups;

    public CleanupPreset(Set<CleanupStep> activeJobs) {
        this(activeJobs, new FieldFormatterCleanups(false, new ArrayList<>()));
    }

    public CleanupPreset(CleanupStep activeJob) {
        this(EnumSet.of(activeJob));
    }

    public CleanupPreset(FieldFormatterCleanups formatterCleanups) {
        this(EnumSet.noneOf(CleanupStep.class), formatterCleanups);
    }

    public CleanupPreset(Set<CleanupStep> activeJobs, FieldFormatterCleanups formatterCleanups) {
        this.activeJobs = activeJobs;
        this.formatterCleanups = Objects.requireNonNull(formatterCleanups);
    }

    public Set<CleanupStep> getActiveJobs() {
        return Collections.unmodifiableSet(activeJobs);
    }

    public boolean isRenamePDFActive() {
        return isActive(CleanupStep.RENAME_PDF) || isActive(CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS);
    }

    public Boolean isActive(CleanupStep step) {
        return activeJobs.contains(step);
    }

    public FieldFormatterCleanups getFormatterCleanups() {
        return formatterCleanups;
    }

    public enum CleanupStep {
        /**
         * Removes the http://... for each DOI. Moves DOIs from URL and NOTE filed to DOI field.
         */
        CLEAN_UP_DOI,
        CLEANUP_EPRINT,
        MAKE_PATHS_RELATIVE,
        RENAME_PDF,
        RENAME_PDF_ONLY_RELATIVE_PATHS,
        /**
         * Collects file links from the pdf or ps field, and adds them to the list contained in the file field.
         */
        CLEAN_UP_UPGRADE_EXTERNAL_LINKS,
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
        CLEAN_UP_ISSN
    }
}
