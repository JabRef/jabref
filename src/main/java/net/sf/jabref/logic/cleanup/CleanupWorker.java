package net.sf.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.cleanup.CleanupJob;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.metadata.FileDirectoryPreferences;

public class CleanupWorker {

    private final BibDatabaseContext databaseContext;
    private final String fileNamePattern;
    private final String fileDirPattern;
    private final LayoutFormatterPreferences prefs;
    private final FileDirectoryPreferences fileDirectoryPreferences;
    private int unsuccessfulRenames;


    public CleanupWorker(BibDatabaseContext databaseContext, CleanupPreferences cleanupPreferences) {
        this.databaseContext = databaseContext;
        this.fileNamePattern = cleanupPreferences.getFileNamePattern();
        this.fileDirPattern = cleanupPreferences.getFileDirPattern();
        this.prefs = cleanupPreferences.getLayoutFormatterPreferences();
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
            jobs.add(new BiblatexCleanup());
        }
        if(preset.getFormatterCleanups().isEnabled()) {
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
            jobs.add(new MoveFilesCleanup(databaseContext, fileDirectoryPreferences));
        }
        if (preset.isMakePathsRelative()) {
            jobs.add(new RelativePathsCleanup(databaseContext, fileDirectoryPreferences));
        }
        if (preset.isRenamePDF()) {
            RenamePdfCleanup cleaner = new RenamePdfCleanup(preset.isRenamePdfOnlyRelativePaths(), databaseContext,
                    fileNamePattern, fileDirPattern, prefs, fileDirectoryPreferences);
            jobs.add(cleaner);
            unsuccessfulRenames += cleaner.getUnsuccessfulRenames();
        }

        return jobs;
    }
}
