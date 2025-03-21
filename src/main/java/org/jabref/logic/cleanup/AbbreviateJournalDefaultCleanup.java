package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class AbbreviateJournalDefaultCleanup implements CleanupJob {
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final boolean useFJournalField;

    public AbbreviateJournalDefaultCleanup(JournalAbbreviationRepository journalAbbreviationRepository, boolean useFJournalField) {
        this.journalAbbreviationRepository = journalAbbreviationRepository;
        this.useFJournalField = useFJournalField;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> allChanges = new ArrayList<>();

        allChanges.addAll(abbreviateField(entry, StandardField.JOURNAL));
        allChanges.addAll(abbreviateField(entry, StandardField.JOURNALTITLE));

        return allChanges;
    }

    private List<FieldChange> abbreviateField(BibEntry entry, Field fieldName) {
        if (!entry.hasField(fieldName)) {
            return Collections.emptyList();
        }

        String oldValue = entry.getField(fieldName).orElse(null);
        Optional<Abbreviation> foundAbbrev = journalAbbreviationRepository.get(oldValue);

        if (foundAbbrev.isEmpty()) {
            return Collections.emptyList();
        }

        Abbreviation abbreviation = foundAbbrev.get();
        String newValue = abbreviation.getAbbreviation(); // “default” abbreviation

        if (newValue.equals(oldValue)) {
            return Collections.emptyList();
        }

        List<FieldChange> changes = new ArrayList<>();

        if (useFJournalField && (StandardField.JOURNAL == fieldName || StandardField.JOURNALTITLE == fieldName)) {
            String oldFjournalValue = entry.getField(AMSField.FJOURNAL).orElse(null);
            String newFjournalValue = abbreviation.getName();
            entry.setField(AMSField.FJOURNAL, newFjournalValue);
            changes.add(new FieldChange(entry, AMSField.FJOURNAL, oldFjournalValue, newFjournalValue));
        }

        entry.setField(fieldName, newValue);
        changes.add(new FieldChange(entry, fieldName, oldValue, newValue));

        return changes;
    }
}
