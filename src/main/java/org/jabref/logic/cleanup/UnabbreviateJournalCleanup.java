package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class UnabbreviateJournalCleanup implements CleanupJob {
    private final BibDatabase database;
    private final JournalAbbreviationRepository journalAbbreviationRepository;

    public UnabbreviateJournalCleanup(BibDatabase database, JournalAbbreviationRepository journalAbbreviationRepository) {
        this.database = database;
        this.journalAbbreviationRepository = journalAbbreviationRepository;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> allChanges = new ArrayList<>();

        allChanges.addAll(unabbreviateField(entry, StandardField.JOURNAL));
        allChanges.addAll(unabbreviateField(entry, StandardField.JOURNALTITLE));

        return allChanges;
    }

    private List<FieldChange> unabbreviateField(BibEntry entry, Field fieldName) {
        if (!entry.hasField(fieldName)) {
            return Collections.emptyList();
        }
        List<FieldChange> changes = new ArrayList<>();

        return changes;
    }
}
