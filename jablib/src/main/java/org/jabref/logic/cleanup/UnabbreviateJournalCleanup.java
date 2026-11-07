package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.conferences.ConferenceAbbreviationRepository;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.AbbreviationRepository;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;

/// Expands journal fields and, when supplied, conference book titles.
@NullMarked
public class UnabbreviateJournalCleanup implements CleanupJob {
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final BibDatabase database;
    private final ConferenceAbbreviationRepository conferenceAbbreviationRepository;

    public UnabbreviateJournalCleanup(BibDatabase database, JournalAbbreviationRepository journalAbbreviationRepository) {
        this(database, journalAbbreviationRepository, new ConferenceAbbreviationRepository());
    }

    public UnabbreviateJournalCleanup(BibDatabase database, JournalAbbreviationRepository journalAbbreviationRepository, ConferenceAbbreviationRepository conferenceAbbreviationRepository) {
        this.conferenceAbbreviationRepository = conferenceAbbreviationRepository;
        this.database = database;
        this.journalAbbreviationRepository = journalAbbreviationRepository;
    }

    // [impl->req~abbreviations.booktitle~1]
    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> allChanges = new ArrayList<>();

        allChanges.addAll(unabbreviate(entry, StandardField.JOURNAL, journalAbbreviationRepository));
        allChanges.addAll(unabbreviate(entry, StandardField.JOURNALTITLE, journalAbbreviationRepository));

        allChanges.addAll(unabbreviate(entry, StandardField.BOOKTITLE, conferenceAbbreviationRepository));
        return allChanges;
    }

    private List<FieldChange> unabbreviate(BibEntry entry, Field field, AbbreviationRepository repository) {
        if (!entry.hasField(field)) {
            return List.of();
        }

        List<FieldChange> changes = new ArrayList<>(restoreUnabbreviatedJournalTitleFromFJournal(entry, field));
        if (!changes.isEmpty()) {
            return changes;
        }

        String text = entry.getFieldLatexFree(field).orElse("");
        String origText = text;
        text = database.resolveForStrings(origText);

        if (!repository.isKnownName(text)) {
            return List.of(); // Cannot do anything if it is not known.
        }

        if (!repository.isAbbreviatedName(text)) {
            return List.of(); // Cannot unabbreviate unabbreviated name.
        }

        Abbreviation abbreviation = repository.get(text).orElseThrow();
        String newText = abbreviation.getName().replaceAll("(?<!\\\\)&", "\\\\&");
        entry.setField(field, newText);
        changes.add(new FieldChange(entry, field, origText, newText));
        return changes;
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
