package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.application.Platform;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class CleanupWorker {

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final TimestampPreferences timestampPreferences;
    private final DialogService dialogService;
    private final List<JabRefException> fileMoveExceptions;

    public CleanupWorker(BibDatabaseContext databaseContext, FilePreferences filePreferences, TimestampPreferences timestampPreferences, DialogService dialogService) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
        this.timestampPreferences = timestampPreferences;
        this.dialogService = dialogService;
        this.fileMoveExceptions = new ArrayList<>();
    }

    public List<FieldChange> cleanup(CleanupPreferences preset, BibEntry entry) {
        Objects.requireNonNull(preset);
        Objects.requireNonNull(entry);

        List<CleanupJob> jobs = determineCleanupActions(preset);

        List<FieldChange> changes = new ArrayList<>();
        for (CleanupJob job : jobs) {
            changes.addAll(job.cleanup(entry));

            if (job instanceof MoveFilesCleanup cleanup) {
                fileMoveExceptions.addAll(cleanup.getIoExceptions());
            }
        }

        if (!fileMoveExceptions.isEmpty()) {
            showFileMoveExceptions(entry);
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
            case CLEAN_UP_URL ->
                    new URLCleanup();
            case MAKE_PATHS_RELATIVE ->
                    new RelativePathsCleanup(databaseContext, filePreferences);
            case RENAME_PDF ->
                    new RenamePdfCleanup(false, databaseContext, filePreferences);
            case RENAME_PDF_ONLY_RELATIVE_PATHS ->
                    new RenamePdfCleanup(true, databaseContext, filePreferences);
            case CLEAN_UP_UPGRADE_EXTERNAL_LINKS ->
                    new UpgradePdfPsToFileCleanup();
            case CLEAN_UP_DELETED_LINKED_FILES ->
                    new RemoveLinksToNotExistentFiles(databaseContext, filePreferences);
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

    private void showFileMoveExceptions(BibEntry entry) {
        StringBuilder sb = new StringBuilder();
        String title = entry.getTitle().orElse(Localization.lang("Unknown Title"));

        sb.append(Localization.lang("The following errors occurred while moving files associated with the entry '%0':", title)).append("\n\n");
        for (JabRefException exception : fileMoveExceptions) {
            sb.append("- ").append(exception.getLocalizedMessage()).append("\n");
        }
        Platform.runLater(() ->
                dialogService.showErrorDialogAndWait(Localization.lang("File Move Errors"), sb.toString())
        );
    }
}
