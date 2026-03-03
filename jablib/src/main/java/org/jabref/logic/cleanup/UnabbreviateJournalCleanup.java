package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.conferences.ConferenceAbbreviationRepository;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;

/// Unabbreviates journal field
@NullMarked
public class UnabbreviateJournalCleanup implements CleanupJob {
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final BibDatabase database;
    private final ConferenceAbbreviationRepository conferenceAbbreviationRepository;

    public UnabbreviateJournalCleanup(BibDatabase database,
                                      JournalAbbreviationRepository journalAbbreviationRepository,
                                      ConferenceAbbreviationRepository conferenceAbbreviationRepository) {
        this.database = database;
        this.journalAbbreviationRepository = journalAbbreviationRepository;
        this.conferenceAbbreviationRepository = conferenceAbbreviationRepository;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> allChanges = new ArrayList<>();

        allChanges.addAll(unabbreviate(entry, StandardField.JOURNAL));
        allChanges.addAll(unabbreviate(entry, StandardField.JOURNALTITLE));
        allChanges.addAll(unabbreviate(entry, StandardField.BOOKTITLE));

        return allChanges;
    }

    private List<FieldChange> unabbreviate(BibEntry entry, Field field) {
        if (!entry.hasField(field)) {
            return List.of();
        }

        if (StandardField.BOOKTITLE == field) {
            return unabbreviateBookTitle(entry, field);
        }

        List<FieldChange> changes = new ArrayList<>(restoreUnabbreviatedJournalTitleFromFJournal(entry, field));
        if (!changes.isEmpty()) {
            return changes;
        }

        String text = entry.getFieldLatexFree(field).orElse("");
        String origText = text;
        text = database.resolveForStrings(origText);

        if (!journalAbbreviationRepository.isKnownName(text)) {
            return List.of(); // Cannot do anything if it is not known.
        }

        if (!journalAbbreviationRepository.isAbbreviatedName(text)) {
            return List.of(); // Cannot unabbreviate unabbreviated name.
        }

        String newText = journalAbbreviationRepository.get(text)
                                                      .orElseThrow()
                                                      .getName()
                                                      .replaceAll("(?<!\\\\)&", "\\\\&");
        entry.setField(field, newText);
        changes.add(new FieldChange(entry, field, origText, newText));
        return changes;
    }

    private List<FieldChange> unabbreviateBookTitle(BibEntry entry, Field field) {
        String text = entry.getFieldLatexFree(field).orElse("");
        String origText = text;
        text = database.resolveForStrings(origText);

        Optional<String> newTextOptional = conferenceAbbreviationRepository.getFullName(text);
        if (newTextOptional.isEmpty() || newTextOptional.get().equals(origText)) {
            return List.of();
        }

        String newText = newTextOptional.get().replaceAll("(?<!\\\\)&", "\\\\&");
        entry.setField(field, newText);
        return List.of(new FieldChange(entry, field, origText, newText));
    }

    private List<FieldChange> restoreUnabbreviatedJournalTitleFromFJournal(BibEntry entry, Field field) {
        if ((StandardField.JOURNAL != field && StandardField.JOURNALTITLE != field) || !entry.hasField(AMSField.FJOURNAL)) {
            return List.of();
        }

        String newText = entry.getField(AMSField.FJOURNAL).orElse("").trim();
        if (newText.isBlank()) {
            return List.of();
        }

        List<FieldChange> changes = new ArrayList<>();

        String origText = entry.getField(field).orElse("");
        entry.clearField(AMSField.FJOURNAL);
        changes.add(new FieldChange(entry, AMSField.FJOURNAL, newText, null));

        entry.setField(field, newText);
        changes.add(new FieldChange(entry, field, origText, newText));

        return changes;
    }
}
