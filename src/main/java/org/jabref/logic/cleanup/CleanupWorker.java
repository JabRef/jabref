package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.CleanupPreferences;
import org.jabref.preferences.FilePreferences;

public class CleanupWorker {

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final TimestampPreferences timestampPreferences;

    public CleanupWorker(BibDatabaseContext databaseContext, FilePreferences filePreferences, TimestampPreferences timestampPreferences) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
        this.timestampPreferences = timestampPreferences;
    }

    public List<FieldChange> cleanup(CleanupPreferences preset, BibEntry entry) {
        Objects.requireNonNull(preset);
        Objects.requireNonNull(entry);

        List<CleanupJob> jobs = determineCleanupActions(preset);

        List<FieldChange> changes = new ArrayList<>();
        for (CleanupJob job : jobs) {
            changes.addAll(job.cleanup(entry));
        }

        return changes;
    }

    private List<CleanupJob> determineCleanupActions(CleanupPreferences preset) {
        List<CleanupJob> jobs = new ArrayList<>();

        for (CleanupPreferences.CleanupStep action : preset.getActiveJobs()) {
            jobs.add(toJob(action));
        }

        if (preset.getFieldFormatterCleanups().isEnabled()) {
            jobs.addAll(preset.getFieldFormatterCleanups().getConfiguredActions());
        }

        return jobs;
    }

    private CleanupJob toJob(CleanupPreferences.CleanupStep action) {
        return switch (action) {
            case CLEAN_UP_DOI ->
                    new DoiCleanup();
            case CLEANUP_EPRINT ->
                    new EprintCleanup();
            case MAKE_PATHS_RELATIVE ->
                    new RelativePathsCleanup(databaseContext, filePreferences);
            case RENAME_PDF ->
                    new RenamePdfCleanup(false, databaseContext, filePreferences);
            case RENAME_PDF_ONLY_RELATIVE_PATHS ->
                    new RenamePdfCleanup(true, databaseContext, filePreferences);
            case CLEAN_UP_UPGRADE_EXTERNAL_LINKS ->
                    new UpgradePdfPsToFileCleanup();
            case CONVERT_TO_BIBLATEX ->
                    new ConvertToBiblatexCleanup();
            case CONVERT_TO_BIBTEX ->
                    new ConvertToBibtexCleanup();
            case CONVERT_TIMESTAMP_TO_CREATIONDATE ->
                    new TimeStampToCreationDate(timestampPreferences);
            case CONVERT_TIMESTAMP_TO_MODIFICATIONDATE ->
                    new TimeStampToModificationDate(timestampPreferences);
            case MOVE_PDF ->
                    new MoveFilesCleanup(databaseContext, filePreferences);
            case FIX_FILE_LINKS ->
                    new FileLinksCleanup();
            case CLEAN_UP_ISSN ->
                    new ISSNCleanup();
            default ->
                    throw new UnsupportedOperationException(action.name());
        };
    }
}
