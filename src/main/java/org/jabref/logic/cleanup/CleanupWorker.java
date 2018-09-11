package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.model.FieldChange;
import org.jabref.model.cleanup.CleanupJob;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.FilePreferences;

public class CleanupWorker {

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;

    public CleanupWorker(BibDatabaseContext databaseContext, CleanupPreferences cleanupPreferences) {
        this.databaseContext = databaseContext;
        this.filePreferences = cleanupPreferences.getFilePreferences();
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

        if (preset.isConvertToBiblatex()) {
            jobs.add(new ConvertToBiblatexCleanup());
        }
        if (preset.isConvertToBibtex()) {
            jobs.add(new ConvertToBibtexCleanup());
        }
        if (preset.getFormatterCleanups().isEnabled()) {
            jobs.addAll(preset.getFormatterCleanups().getConfiguredActions());
        }
        if (preset.isCleanUpUpgradeExternalLinks()) {
            jobs.add(new UpgradePdfPsToFileCleanup());
        }
        if (preset.isCleanUpDOI()) {
            jobs.add(new DoiCleanup());
        }
        if (preset.isCleanUpISSN()) {
            jobs.add(new ISSNCleanup());
        }
        if (preset.isFixFileLinks()) {
            jobs.add(new FileLinksCleanup());
        }
        if (preset.isMovePDF()) {
            jobs.add(new MoveFilesCleanup(databaseContext, filePreferences));
        }
        if (preset.isMakePathsRelative()) {
            jobs.add(new RelativePathsCleanup(databaseContext, filePreferences));
        }
        if (preset.isRenamePDF()) {
            RenamePdfCleanup cleaner = new RenamePdfCleanup(preset.isRenamePdfOnlyRelativePaths(), databaseContext, filePreferences);
            jobs.add(cleaner);
        }

        return jobs;
    }
}
