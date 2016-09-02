package net.sf.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.FileDirectoryPreferences;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class CleanupWorker {

    private final BibDatabaseContext databaseContext;
    private final String fileNamePattern;
    private final LayoutFormatterPreferences prefs;
    private final FileDirectoryPreferences fileDirectoryPreferences;
    private int unsuccessfulRenames;


    public CleanupWorker(BibDatabaseContext databaseContext, String fileNamePattern, LayoutFormatterPreferences prefs,
            FileDirectoryPreferences fileDirectoryPreferences) {
        this.databaseContext = databaseContext;
        this.fileNamePattern = fileNamePattern;
        this.prefs = prefs;
        this.fileDirectoryPreferences = fileDirectoryPreferences;
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

        if (preset.isCleanUpUpgradeExternalLinks()) {
            jobs.add(new UpgradePdfPsToFileCleanup(Arrays.asList(FieldName.PDF, FieldName.PS)));
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
                    fileNamePattern, prefs, fileDirectoryPreferences);
            jobs.add(cleaner);
            unsuccessfulRenames += cleaner.getUnsuccessfulRenames();
        }
        if (preset.isConvertToBiblatex()) {
            jobs.add(new BiblatexCleanup());
        }

        if(preset.getFormatterCleanups().isEnabled()) {
            jobs.addAll(preset.getFormatterCleanups().getConfiguredActions());
        }

        return jobs;
    }
}
