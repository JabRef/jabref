package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

/**
 * Unabbreviates journal field.
 */
public class UnabbreviateJournalCleanup implements CleanupJob {
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final BibDatabase database;

    public UnabbreviateJournalCleanup(BibDatabase database, JournalAbbreviationRepository journalAbbreviationRepository) {
        this.database = database;
        this.journalAbbreviationRepository = journalAbbreviationRepository;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> allChanges = new ArrayList<>();

        allChanges.addAll(unabbreviate(entry, StandardField.JOURNAL));
        allChanges.addAll(unabbreviate(entry, StandardField.JOURNALTITLE));

        return allChanges;
    }

    private List<FieldChange> unabbreviate(BibEntry entry, Field field) {
        if (!entry.hasField(field)) {
            return List.of();
        }

        List<FieldChange> changes = new ArrayList<>(restoreFromFJournal(entry, field));

        if (!changes.isEmpty()) {
            return changes;
        }

        String text = entry.getFieldLatexFree(field).orElse("");
        String origText = text;
        text = database != null ? database.resolveForStrings(origText) : origText;

        if (!journalAbbreviationRepository.isKnownName(text)) {
            return List.of(); // Cannot do anything if it is not known.
        }

        if (!journalAbbreviationRepository.isAbbreviatedName(text)) {
            return List.of(); // Cannot unabbreviate unabbreviated name.
        }

        Abbreviation abbreviation = journalAbbreviationRepository.get(text).orElseThrow();
        String newText = abbreviation.getName();
        entry.setField(field, newText);
        changes.add(new FieldChange(entry, field, origText, newText));
        return changes;
    }

    private List<FieldChange> restoreFromFJournal(BibEntry entry, Field field) {
        if ((StandardField.JOURNAL != field && StandardField.JOURNALTITLE != field) || !entry.hasField(AMSField.FJOURNAL)) {
            return List.of();
        }

        String newText = entry.getField(AMSField.FJOURNAL).orElse("");
        if (newText.isBlank()) {
            return List.of();
        }

        List<FieldChange> changes = new ArrayList<>();

        String origText = entry.getField(field).orElse("");

        entry.setField(AMSField.FJOURNAL, "");
        changes.add(new FieldChange(entry, AMSField.FJOURNAL, newText, ""));

        entry.setField(field, newText);
        changes.add(new FieldChange(entry, field, origText, newText));

        return changes;
    }
}
