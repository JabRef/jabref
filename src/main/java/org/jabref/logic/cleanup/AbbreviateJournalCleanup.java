package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.AbbreviationType;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class AbbreviateJournalCleanup implements CleanupJob {
    private final BibDatabase database;
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final AbbreviationType abbreviationType;
    private final boolean useFJournalField;

    public AbbreviateJournalCleanup(BibDatabase database, JournalAbbreviationRepository journalAbbreviationRepository, AbbreviationType abbreviationType, boolean useFJournalField) {
        this.database = database;
        this.journalAbbreviationRepository = journalAbbreviationRepository;
        this.abbreviationType = abbreviationType;
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

        String text = entry.getField(fieldName).orElse(null);
        String originalText = text;
        if (database != null) {
            text = database.resolveForStrings(text);
        }
        Optional<Abbreviation> foundAbbrev = journalAbbreviationRepository.get(text);

        if (foundAbbrev.isEmpty()) {
            return Collections.emptyList();
        }

        Abbreviation abbreviation = foundAbbrev.get();
        String newText = getAbbreviatedName(abbreviation);

        if (newText.equals(originalText)) {
            return Collections.emptyList();
        }

        List<FieldChange> changes = new ArrayList<>();

        if (useFJournalField && (StandardField.JOURNAL == fieldName || StandardField.JOURNALTITLE == fieldName)) {
            String oldFjournalValue = entry.getField(AMSField.FJOURNAL).orElse(null);
            String newFjournalValue = abbreviation.getName();
            entry.setField(AMSField.FJOURNAL, newFjournalValue);
            changes.add(new FieldChange(entry, AMSField.FJOURNAL, oldFjournalValue, newFjournalValue));
        }

        entry.setField(fieldName, newText);
        changes.add(new FieldChange(entry, fieldName, originalText, newText));

        return changes;
    }

    private String getAbbreviatedName(Abbreviation text) {
        switch (abbreviationType) {
            case DEFAULT -> {
                return text.getAbbreviation();
            }
            case DOTLESS -> {
                return text.getDotlessAbbreviation();
            }
            case SHORTEST_UNIQUE -> {
                return text.getShortestUniqueAbbreviation();
            }
        }
        // Should never happen
        return text.getName();
    }
}
