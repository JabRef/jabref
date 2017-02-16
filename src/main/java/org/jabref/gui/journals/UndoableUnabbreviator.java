package org.jabref.gui.journals;

import javax.swing.undo.CompoundEdit;

import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

public class UndoableUnabbreviator {

    private final JournalAbbreviationRepository journalAbbreviationRepository;


    public UndoableUnabbreviator(JournalAbbreviationRepository journalAbbreviationRepository) {
        this.journalAbbreviationRepository = journalAbbreviationRepository;
    }

    /**
     * Unabbreviate the journal name of the given entry.
     *
     * @param entry     The entry to be treated.
     * @param fieldName The field name (e.g. "journal")
     * @param ce        If the entry is changed, add an edit to this compound.
     * @return true if the entry was changed, false otherwise.
     */
    public boolean unabbreviate(BibDatabase database, BibEntry entry, String fieldName, CompoundEdit ce) {
        if (!entry.hasField(fieldName)) {
            return false;
        }
        String text = entry.getField(fieldName).get();
        String origText = text;
        if (database != null) {
            text = database.resolveForStrings(text);
        }

        if (!journalAbbreviationRepository.isKnownName(text)) {
            return false; // cannot do anything if it is not known
        }

        if (!journalAbbreviationRepository.isAbbreviatedName(text)) {
            return false; // cannot unabbreviate unabbreviated name.
        }

        Abbreviation abbreviation = journalAbbreviationRepository.getAbbreviation(text).get(); // must be here
        String newText = abbreviation.getName();
        entry.setField(fieldName, newText);
        ce.addEdit(new UndoableFieldChange(entry, fieldName, origText, newText));
        return true;
    }

}
