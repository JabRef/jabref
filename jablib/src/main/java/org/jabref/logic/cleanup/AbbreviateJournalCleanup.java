package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.conferences.ConferenceAbbreviationRepository;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.AbbreviationType;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;

/// Abbreviates journal field
@NullMarked
public class AbbreviateJournalCleanup implements CleanupJob {
    private final BibDatabase database;
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final AbbreviationType abbreviationType;
    private final ConferenceAbbreviationRepository conferenceAbbreviationRepository;
    private final boolean useFJournalField;

    public AbbreviateJournalCleanup(BibDatabase database, JournalAbbreviationRepository journalAbbreviationRepository, ConferenceAbbreviationRepository conferenceAbbreviationRepository, AbbreviationType abbreviationType, boolean useFJournalField) {
        this.database = database;
        this.journalAbbreviationRepository = journalAbbreviationRepository;
        this.conferenceAbbreviationRepository = conferenceAbbreviationRepository;
        this.abbreviationType = abbreviationType;
        this.useFJournalField = useFJournalField;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> allChanges = new ArrayList<>();
        // Journal is BibTeX, JournalTitle is BibLaTeX. See also org/jabref/model/entry/EntryConverter.java:20
        allChanges.addAll(abbreviateField(entry, StandardField.JOURNAL));
        allChanges.addAll(abbreviateField(entry, StandardField.JOURNALTITLE));
        allChanges.addAll(abbreviateBookTitle(entry));
        return allChanges;
    }

    private List<FieldChange> abbreviateBookTitle(BibEntry entry) {
        if (!entry.hasField(StandardField.BOOKTITLE)) {
            return List.of();
        }

        String origText = entry.getField(StandardField.BOOKTITLE).orElse("");
        String text = database.resolveForStrings(origText);

        Optional<String> newTextOptional = conferenceAbbreviationRepository.getAbbreviation(text);
        if (newTextOptional.isEmpty() || newTextOptional.get().equals(origText)) {
            return List.of();
        }

        String newText = newTextOptional.get().replaceAll("(?<!\\\\)&", "\\\\&");
        entry.setField(StandardField.BOOKTITLE, newText);
        return List.of(new FieldChange(entry, StandardField.BOOKTITLE, origText, newText));
    }

    private List<FieldChange> abbreviateField(BibEntry entry, Field fieldName) {
        if (!entry.hasField(fieldName)) {
            return List.of();
        }

        String origText = entry.getField(fieldName).orElse("");
        String text = database.resolveForStrings(origText);

        Optional<Abbreviation> foundAbbreviation = journalAbbreviationRepository.get(text);

        if (foundAbbreviation.isEmpty() && abbreviationType != AbbreviationType.LTWA) {
            // Not found abbreviation -> cannot abbreviate anything.
            // LTWA mode -> handled differently
            return List.of();
        }

        Optional<String> newTextOptional = abbreviationType == AbbreviationType.LTWA
                                           ? journalAbbreviationRepository.getLtwaAbbreviation(text)
                                           : foundAbbreviation.map(this::getAbbreviatedName);

        // Return early if no abbreviation found or it matches original
        if (newTextOptional.isEmpty() || newTextOptional.get().equals(origText)) {
            return List.of();
        }

        // TODO: Currently FieldWriter does not do escaping. We also don't want the UI for editing journal abbreviations to deal with escapings. Thus, we need to do it here.
        String newText = newTextOptional.get().replaceAll("(?<!\\\\)&", "\\\\&");

        List<FieldChange> changes = new ArrayList<>(2);

        foundAbbreviation.ifPresent(abbr -> {
            if (useFJournalField && (StandardField.JOURNAL == fieldName || StandardField.JOURNALTITLE == fieldName)) {
                String oldFjournalValue = entry.getField(AMSField.FJOURNAL).orElse(null);
                String newFjournalValue = abbr.getName().replaceAll("(?<!\\\\)&", "\\\\&");
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
            case LTWA ->
                    throw new IllegalStateException("Unexpected value: %s".formatted(abbreviationType));
        };
    }
}
