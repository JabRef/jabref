package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.FilePreferences;

public class CleanupWorker {

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final TimestampPreferences timestampPreferences;

    public CleanupWorker(BibDatabaseContext databaseContext, CleanupPreferences cleanupPreferences, TimestampPreferences timestampPreferences) {
        this.databaseContext = databaseContext;
        this.filePreferences = cleanupPreferences.getFilePreferences();
        this.timestampPreferences = timestampPreferences;
    }

    public List<FieldChange> cleanup(CleanupPreset preset, BibEntry entry) {
        Objects.requireNonNull(preset);
        Objects.requireNonNull(entry);

        List<CleanupJob> jobs = determineCleanupActions(preset);

        List<FieldChange> changes = new ArrayList<>();
        for (CleanupJob job : jobs) {
            changes.addAll(job.cleanup(entry));
        }

        return changes;
    }

    private List<CleanupJob> determineCleanupActions(CleanupPreset preset) {
        List<CleanupJob> jobs = new ArrayList<>();

        for (CleanupPreset.CleanupStep action : preset.getActiveJobs()) {
            jobs.add(toJob(action));
        }

        if (preset.getFormatterCleanups().isEnabled()) {
            jobs.addAll(preset.getFormatterCleanups().getConfiguredActions());
        }

        return jobs;
    }

    private CleanupJob toJob(CleanupPreset.CleanupStep action) {
        switch (action) {
            case CLEAN_UP_DOI:
                return new DoiCleanup();
            case CLEANUP_EPRINT:
                return new EprintCleanup();
            case MAKE_PATHS_RELATIVE:
                return new RelativePathsCleanup(databaseContext, filePreferences);
            case RENAME_PDF:
                return new RenamePdfCleanup(false, databaseContext, filePreferences);
            case RENAME_PDF_ONLY_RELATIVE_PATHS:
                return new RenamePdfCleanup(true, databaseContext, filePreferences);
            case CLEAN_UP_UPGRADE_EXTERNAL_LINKS:
                return new UpgradePdfPsToFileCleanup();
            case CONVERT_TO_BIBLATEX:
                return new ConvertToBiblatexCleanup();
            case CONVERT_TO_BIBTEX:
                return new ConvertToBibtexCleanup();
            case CONVERT_TIMESTAMP_TO_CREATIONDATE:
                return new TimeStampToCreationDate(timestampPreferences);
            case CONVERT_TIMESTAMP_TO_MODIFICATIONDATE:
                return new TimeStampToModificationDate(timestampPreferences);
            case MOVE_PDF:
                return new MoveFilesCleanup(databaseContext, filePreferences);
            case FIX_FILE_LINKS:
                return new FileLinksCleanup();
            case CLEAN_UP_ISSN:
                return new ISSNCleanup();
            default:
                throw new UnsupportedOperationException(action.name());
        }
    }
}
