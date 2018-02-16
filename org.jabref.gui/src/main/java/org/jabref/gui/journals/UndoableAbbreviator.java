package org.jabref.gui.journals;

import javax.swing.undo.CompoundEdit;

import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

public class UndoableAbbreviator {

    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final boolean isoAbbreviationStyle;


    public UndoableAbbreviator(JournalAbbreviationRepository journalAbbreviationRepository, boolean isoAbbreviationStyle) {
        this.journalAbbreviationRepository = journalAbbreviationRepository;
        this.isoAbbreviationStyle = isoAbbreviationStyle;
    }

    /**
     * Abbreviate the journal name of the given entry.
     *
     * @param database  The database the entry belongs to, or null if no database.
     * @param entry     The entry to be treated.
     * @param fieldName The field name (e.g. "journal")
     * @param ce        If the entry is changed, add an edit to this compound.
     * @return true if the entry was changed, false otherwise.
     */
    public boolean abbreviate(BibDatabase database, BibEntry entry, String fieldName, CompoundEdit ce) {
        if (!entry.hasField(fieldName)) {
            return false;
        }
        String text = entry.getField(fieldName).get();
        String origText = text;
        if (database != null) {
            text = database.resolveForStrings(text);
        }

        if (!journalAbbreviationRepository.isKnownName(text)) {
            return false; // unknown, cannot un/abbreviate anything
        }

        String newText = getAbbreviatedName(journalAbbreviationRepository.getAbbreviation(text).get());

        if (newText.equals(origText)) {
            return false;
        }

        entry.setField(fieldName, newText);
        ce.addEdit(new UndoableFieldChange(entry, fieldName, origText, newText));
        return true;
    }

    private String getAbbreviatedName(Abbreviation text) {
        if (isoAbbreviationStyle) {
            return text.getIsoAbbreviation();
        } else {
            return text.getMedlineAbbreviation();
        }
    }

}
