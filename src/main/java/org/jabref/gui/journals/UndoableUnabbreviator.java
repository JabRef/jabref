package org.jabref.gui.journals;

import javax.swing.undo.CompoundEdit;

import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class UndoableUnabbreviator {

    private final JournalAbbreviationRepository journalAbbreviationRepository;

    public UndoableUnabbreviator(JournalAbbreviationRepository journalAbbreviationRepository) {
        this.journalAbbreviationRepository = journalAbbreviationRepository;
    }

    /**
     * Unabbreviate the journal name of the given entry.
     *
     * @param entry The entry to be treated.
     * @param field The field
     * @param ce    If the entry is changed, add an edit to this compound.
     * @return true if the entry was changed, false otherwise.
     */
    public boolean unabbreviate(BibDatabase database, BibEntry entry, Field field, CompoundEdit ce) {
        if (!entry.hasField(field)) {
            return false;
        }

        String text = entry.getField(field).get();
        String origText = text;
        if (database != null) {
            text = database.resolveForStrings(text);
        }

        if (!journalAbbreviationRepository.isKnownName(text)) {
            return false; // Cannot do anything if it is not known.
        }

        if (!journalAbbreviationRepository.isAbbreviatedName(text)) {
            return false; // Cannot unabbreviate unabbreviated name.
        }

        Abbreviation abbreviation = journalAbbreviationRepository.get(text).get(); // Must be here.
        String newText = abbreviation.getName();
        entry.setField(field, newText);
        ce.addEdit(new UndoableFieldChange(entry, field, origText, newText));
        return true;
    }
}
