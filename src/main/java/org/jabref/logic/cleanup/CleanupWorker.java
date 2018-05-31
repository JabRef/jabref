package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.cleanup.CleanupJob;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.FileDirectoryPreferences;

public class CleanupWorker {

    private final BibDatabaseContext databaseContext;
    private final String fileNamePattern;
    private final String fileDirPattern;
    private final LayoutFormatterPreferences layoutPrefs;
    private final FileDirectoryPreferences fileDirectoryPreferences;
    private int unsuccessfulRenames;


    public CleanupWorker(BibDatabaseContext databaseContext, CleanupPreferences cleanupPreferences) {
        this.databaseContext = databaseContext;
        this.fileNamePattern = cleanupPreferences.getFileNamePattern();
        this.fileDirPattern = cleanupPreferences.getFileDirPattern();
        this.layoutPrefs = cleanupPreferences.getLayoutFormatterPreferences();
        this.fileDirectoryPreferences = cleanupPreferences.getFileDirectoryPreferences();
    }

    public int getUnsuccessfulRenames() {
        return unsuccessfulRenames;
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
            jobs.add(new MoveFilesCleanup(databaseContext, fileDirPattern, fileDirectoryPreferences, layoutPrefs));
        }
        if (preset.isMakePathsRelative()) {
            jobs.add(new RelativePathsCleanup(databaseContext, fileDirectoryPreferences));
        }
        if (preset.isRenamePDF()) {
            RenamePdfCleanup cleaner = new RenamePdfCleanup(preset.isRenamePdfOnlyRelativePaths(), databaseContext,
                    fileNamePattern, layoutPrefs, fileDirectoryPreferences);
            jobs.add(cleaner);
            unsuccessfulRenames += cleaner.getUnsuccessfulRenames();
        }

        return jobs;
    }
}
