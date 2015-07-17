package net.sf.jabref.journals;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.journals.logic.Abbreviation;
import net.sf.jabref.journals.logic.JournalAbbreviationRepository;
import net.sf.jabref.undo.UndoableFieldChange;

import javax.swing.undo.CompoundEdit;

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
    public boolean unabbreviate(BibtexDatabase database, BibtexEntry entry,
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
