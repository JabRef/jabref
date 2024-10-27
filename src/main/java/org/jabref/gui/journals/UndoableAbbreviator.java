package org.jabref.gui.journals;

import javax.swing.undo.CompoundEdit;

import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

// Undo redo stuff
public class UndoableAbbreviator {

    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final AbbreviationType abbreviationType;
    private final boolean useFJournalField;

    public UndoableAbbreviator(JournalAbbreviationRepository journalAbbreviationRepository, AbbreviationType abbreviationType, boolean useFJournalField) {
        this.journalAbbreviationRepository = journalAbbreviationRepository;
        this.abbreviationType = abbreviationType;
        this.useFJournalField = useFJournalField;
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
    public boolean abbreviate(BibDatabase database, BibEntry entry, Field fieldName, CompoundEdit ce) {
        if (!entry.hasField(fieldName)) {
            return false;
        }

        String originalName = getOriginalName(entry, fieldName, database);
        if (!journalAbbreviationRepository.isKnownName(originalName)) {
            return false;
        }

        String abbreviatedName = getAbbreviatedName(journalAbbreviationRepository.get(originalName).get());
        if (abbreviatedName.equals(originalName)) {
            return false;
        }

        updateFields(entry, fieldName, originalName, abbreviatedName, compoundEdit);
        return true;
    }
    
    private String getOriginalName(BibEntry entry, Field fieldName, BibDatabase database) {
        String name = entry.getField(fieldName).get();
        return (database != null) ? database.resolveForStrings(name) : name;
    }

    private void updateFields(BibEntry entry, Field fieldName, String originalName, String newName, CompoundEdit compoundEdit) {
        if (useFJournalField && (StandardField.JOURNAL == fieldName || StandardField.JOURNALTITLE == fieldName)) {
            entry.setField(AMSField.FJOURNAL, journalAbbreviationRepository.get(originalName).get().getName());
            compoundEdit.addEdit(new UndoableFieldChange(entry, AMSField.FJOURNAL, null, journalAbbreviationRepository.get(originalName).get().getName()));
        }
        entry.setField(fieldName, newName);
        compoundEdit.addEdit(new UndoableFieldChange(entry, fieldName, originalName, newName));
    }

    private String getAbbreviatedName(Abbreviation text) {
        return switch (abbreviationType) {
            case DEFAULT ->
                    text.getAbbreviation();
            case DOTLESS ->
                    text.getDotlessAbbreviation();
            case SHORTEST_UNIQUE ->
                    text.getShortestUniqueAbbreviation();
            default ->
                    throw new IllegalStateException("Unexpected value: %s".formatted(abbreviationType));
        };
    }
}
