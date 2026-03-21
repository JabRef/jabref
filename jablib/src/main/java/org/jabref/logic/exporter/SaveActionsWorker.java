package org.jabref.logic.exporter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupWorker;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.cleanup.NormalizeWhitespacesCleanup;
import org.jabref.logic.formatter.bibtexfields.TrimWhitespaceFormatter;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.metadata.MetaData;

public class SaveActionsWorker {
    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final TimestampPreferences timestampPreferences;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final boolean useFJournalField;
    private final FieldPreferences fieldPreferences;

    public SaveActionsWorker(BibDatabaseContext databaseContext, FilePreferences filePreferences, TimestampPreferences timestampPreferences, FieldPreferences fieldPreferences, boolean useFJournalField, JournalAbbreviationRepository abbreviationRepository) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
        this.timestampPreferences = timestampPreferences;
        this.abbreviationRepository = abbreviationRepository;
        this.useFJournalField = useFJournalField;
        this.fieldPreferences = fieldPreferences;
    }

    public List<FieldChange> applySaveActions(List<BibEntry> toChange, MetaData metaData) {
        List<FieldChange> changes = new ArrayList<>();

        EnumSet<CleanupPreferences.CleanupStep> activeJobs = EnumSet.noneOf(CleanupPreferences.CleanupStep.class);

        Optional<Set<CleanupPreferences.CleanupStep>> multiFieldCleanups = metaData.getMultiFieldCleanups();
        multiFieldCleanups.ifPresent(activeJobs::addAll);

        Optional<CleanupPreferences.CleanupStep> journalAbbreviationCleanup = metaData.getJournalAbbreviationCleanup();
        journalAbbreviationCleanup.ifPresent(activeJobs::add);

        CleanupPreferences cleanupPreferences = new CleanupPreferences(activeJobs);

        Optional<FieldFormatterCleanupActions> fieldFormatterCleanupActions = metaData.getFieldFormatterCleanupActions();
        fieldFormatterCleanupActions.ifPresent(cleanupPreferences::setFieldFormatterCleanups);

        CleanupWorker cleanupWorker = new CleanupWorker(databaseContext, filePreferences, timestampPreferences, useFJournalField, abbreviationRepository);
        for (BibEntry entry : toChange) {
            changes.addAll(cleanupWorker.cleanup(cleanupPreferences, entry));
        }

        // Trim and normalize all white spaces
        FieldFormatterCleanup trimWhiteSpaces = new FieldFormatterCleanup(InternalField.INTERNAL_ALL_FIELD, new TrimWhitespaceFormatter());
        NormalizeWhitespacesCleanup normalizeWhitespacesCleanup = new NormalizeWhitespacesCleanup(fieldPreferences);
        for (BibEntry entry : toChange) {
            // Only apply the trimming if the entry itself has other changes (e.g., by the user or by save actions)
            if (entry.hasChanged()) {
                changes.addAll(trimWhiteSpaces.cleanup(entry));
                changes.addAll(normalizeWhitespacesCleanup.cleanup(entry));
            }
        }

        return changes;
    }

    public List<FieldChange> applySaveActions(BibEntry toChange, MetaData metaData) {
        return applySaveActions(List.of(toChange), metaData);
    }
}
