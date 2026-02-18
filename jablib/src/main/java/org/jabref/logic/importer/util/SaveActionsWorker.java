package org.jabref.logic.importer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jabref.logic.JabRefException;
import org.jabref.logic.cleanup.AbbreviateJournalCleanup;
import org.jabref.logic.cleanup.CleanupJob;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.ConvertToBiblatexCleanup;
import org.jabref.logic.cleanup.ConvertToBibtexCleanup;
import org.jabref.logic.cleanup.DoiCleanup;
import org.jabref.logic.cleanup.EprintCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.cleanup.TimeStampToCreationDate;
import org.jabref.logic.cleanup.TimeStampToModificationDate;
import org.jabref.logic.cleanup.URLCleanup;
import org.jabref.logic.cleanup.UnabbreviateJournalCleanup;
import org.jabref.logic.journals.AbbreviationType;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveActionsWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveActionsWorker.class);

    private final BibDatabaseContext databaseContext;
    private final TimestampPreferences timestampPreferences;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final boolean useFJournalField;
    private final List<JabRefException> failures;

    public SaveActionsWorker(BibDatabaseContext databaseContext, TimestampPreferences timestampPreferences, boolean useFJournalField, JournalAbbreviationRepository abbreviationRepository) {
        this.databaseContext = databaseContext;
        this.timestampPreferences = timestampPreferences;
        this.abbreviationRepository = abbreviationRepository;
        this.useFJournalField = useFJournalField;
        this.failures = new ArrayList<>();
    }

    public List<FieldChange> applySaveActions(Set<CleanupPreferences.CleanupStep> steps, BibEntry entry) {
        List<CleanupJob> jobs = determineCleanupActions(steps);
        List<FieldChange> changes = new ArrayList<>();
        for (CleanupJob job : jobs) {
            changes.addAll(job.cleanup(entry));
        }
        return changes;
    }

    public List<FieldChange> applySaveActions(FieldFormatterCleanupActions actions, BibEntry entry) {
        return actions.applySaveActions(entry);
    }

    private List<CleanupJob> determineCleanupActions(Set<CleanupPreferences.CleanupStep> steps) {
        List<CleanupJob> jobs = new ArrayList<>();

        for (CleanupPreferences.CleanupStep action : steps) {
            jobs.add(toJob(action));
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
            case CONVERT_TO_BIBLATEX ->
                    new ConvertToBiblatexCleanup();
            case CONVERT_TO_BIBTEX ->
                    new ConvertToBibtexCleanup();
            case CONVERT_TIMESTAMP_TO_CREATIONDATE ->
                    new TimeStampToCreationDate(timestampPreferences);
            case CONVERT_TIMESTAMP_TO_MODIFICATIONDATE ->
                    new TimeStampToModificationDate(timestampPreferences);
            case ABBREVIATE_DEFAULT ->
                    new AbbreviateJournalCleanup(databaseContext.getDatabase(), abbreviationRepository, AbbreviationType.DEFAULT, useFJournalField);
            case ABBREVIATE_DOTLESS ->
                    new AbbreviateJournalCleanup(databaseContext.getDatabase(), abbreviationRepository, AbbreviationType.DOTLESS, useFJournalField);
            case ABBREVIATE_SHORTEST_UNIQUE ->
                    new AbbreviateJournalCleanup(databaseContext.getDatabase(), abbreviationRepository, AbbreviationType.SHORTEST_UNIQUE, useFJournalField);
            case ABBREVIATE_LTWA ->
                    new AbbreviateJournalCleanup(databaseContext.getDatabase(), abbreviationRepository, AbbreviationType.LTWA, useFJournalField);
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
