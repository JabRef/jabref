package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.gui.journals.AbbreviationType;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class CleanupWorker {

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final TimestampPreferences timestampPreferences;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final boolean useFJounalField;
    private final List<JabRefException> failures;

    public CleanupWorker(BibDatabaseContext databaseContext, FilePreferences filePreferences, TimestampPreferences timestampPreferences, boolean useFJournalField, JournalAbbreviationRepository abbreviationRepository) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
        this.timestampPreferences = timestampPreferences;
        this.abbreviationRepository = abbreviationRepository;
        this.useFJounalField = useFJournalField;
        this.failures = new ArrayList<>();
    }

    public List<FieldChange> cleanup(CleanupPreferences preset, BibEntry entry) {
        Objects.requireNonNull(preset);
        Objects.requireNonNull(entry);

        List<CleanupJob> jobs = determineCleanupActions(preset);
        List<FieldChange> changes = new ArrayList<>();
        for (CleanupJob job : jobs) {
            changes.addAll(job.cleanup(entry));
            if (job instanceof MoveFilesCleanup cleanup) {
                failures.addAll(cleanup.getIoExceptions());
            }
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
                    new RenamePdfCleanup(false, () -> databaseContext, filePreferences);
            case RENAME_PDF_ONLY_RELATIVE_PATHS ->
                    new RenamePdfCleanup(true, () -> databaseContext, filePreferences);
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
                    new MoveFilesCleanup(() -> databaseContext, filePreferences);
            case FIX_FILE_LINKS ->
                    new FileLinksCleanup();
            case CLEAN_UP_ISSN ->
                    new ISSNCleanup();
            case ABBREVIATE_DEFAULT ->
                    new AbbreviateJournalCleanup(databaseContext.getDatabase(), abbreviationRepository, AbbreviationType.DEFAULT, useFJounalField);
            case ABBREVIATE_DOTLESS ->
                    new AbbreviateJournalCleanup(databaseContext.getDatabase(), abbreviationRepository, AbbreviationType.DOTLESS, useFJounalField);
            case ABBREVIATE_SHORTEST_UNIQUE ->
                    new AbbreviateJournalCleanup(databaseContext.getDatabase(), abbreviationRepository, AbbreviationType.SHORTEST_UNIQUE, useFJounalField);
            case UNABBREVIATE ->
                    new UnabbreviateJournalCleanup(databaseContext.getDatabase(), abbreviationRepository);
            default ->
                    throw new UnsupportedOperationException(action.name());
        };
    }

    public List<JabRefException> getFailures() {
        return failures;
    }
}
