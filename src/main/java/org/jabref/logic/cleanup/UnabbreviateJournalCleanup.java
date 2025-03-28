package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
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

        // Access to FJournal allows shortcut
        if ((StandardField.JOURNAL == fieldName || StandardField.JOURNALTITLE == fieldName) && entry.hasField(AMSField.FJOURNAL)) {
            return restoreUnabbreviatedJournalTitleFromFJournal(entry, fieldName, changes);
        }

        // No access to FJournal
        String text = entry.getFieldLatexFree(fieldName).get();
        String origText = text;
        if (database != null) {
            text = database.resolveForStrings(text);
        }

        if (!journalAbbreviationRepository.isKnownName(text)) {
            return Collections.emptyList(); // Cannot do anything if it is not known.
        }

        if (!journalAbbreviationRepository.isAbbreviatedName(text)) {
            return Collections.emptyList(); // Cannot unabbreviate unabbreviated name.
        }

        Abbreviation abbreviation = journalAbbreviationRepository.get(text).get();
        String newText = abbreviation.getName();
        entry.setField(fieldName, newText);
        changes.add(new FieldChange(entry, fieldName, origText, newText));

        return changes;
    }

    private static List<FieldChange> restoreUnabbreviatedJournalTitleFromFJournal(BibEntry entry, Field fieldName, List<FieldChange> changes) {
        String origText = entry.getField(fieldName).get();
        String newText = entry.getField(AMSField.FJOURNAL).get().trim();

        entry.setField(AMSField.FJOURNAL, "");
        changes.add(new FieldChange(entry, AMSField.FJOURNAL, newText, ""));

        entry.setField(fieldName, newText);
        changes.add(new FieldChange(entry, fieldName, origText, newText));

        return changes;
    }
}
