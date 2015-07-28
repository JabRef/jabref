package net.sf.jabref.journals;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.journals.logic.Abbreviation;
import net.sf.jabref.journals.logic.JournalAbbreviationRepository;
import net.sf.jabref.undo.UndoableFieldChange;

import javax.swing.undo.CompoundEdit;

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
    public boolean abbreviate(BibtexDatabase database, BibtexEntry entry,
                              String fieldName, CompoundEdit ce) {
        String text = entry.getField(fieldName);
        if (text == null) {
            return false;
        }
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
