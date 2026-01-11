package org.jabref.logic.cleanup;

import java.util.ArrayList;
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

/**
 * Abbreviates journal field.
 */
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
            return List.of();
        }

        String origText = entry.getField(fieldName).orElse("");
        String text = database != null ? database.resolveForStrings(origText) : origText;

        List<FieldChange> changes = new ArrayList<>();

        Optional<Abbreviation> foundAbbreviation = journalAbbreviationRepository.get(text);

        if (foundAbbreviation.isEmpty() && abbreviationType != AbbreviationType.LTWA) {
            return List.of(); // Unknown, cannot abbreviate anything.
        }

        Optional<String> newTextOptional = abbreviationType == AbbreviationType.LTWA
                                           ? journalAbbreviationRepository.getLtwaAbbreviation(text)
                                           : foundAbbreviation.map(this::getAbbreviatedName);

        // Return early if no abbreviation found or it matches original
        if (newTextOptional.isEmpty() || newTextOptional.get().equals(origText)) {
            return List.of();
        }

        String newText = newTextOptional.get();

        // Store full name into fjournal but only if it exists
        foundAbbreviation.ifPresent(abbr -> {
            if (useFJournalField && (StandardField.JOURNAL == fieldName || StandardField.JOURNALTITLE == fieldName)) {
                String oldFjournalValue = entry.getField(AMSField.FJOURNAL).orElse(null);
                String newFjournalValue = abbr.getName();
                entry.setField(AMSField.FJOURNAL, newFjournalValue);
                changes.add(new FieldChange(entry, AMSField.FJOURNAL, oldFjournalValue, newFjournalValue));
            }
        });

        entry.setField(fieldName, newText);
        changes.add(new FieldChange(entry, fieldName, origText, newText));
        return changes;
    }

    private String getAbbreviatedName(Abbreviation text) {
        return switch (abbreviationType) {
            case DEFAULT ->
                    text.getAbbreviation();
            case DOTLESS ->
                    text.getDotlessAbbreviation();
            case SHORTEST_UNIQUE ->
                    text.getShortestUniqueAbbreviation();
            default ->
                    throw new IllegalStateException("Unexpected value: %s".formatted(abbreviationType));
        };
    }
}
