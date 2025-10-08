package org.jabref.gui.journals;

import java.util.Optional;

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
     * @param compoundEdit        If the entry is changed, add an edit to this compound.
     * @return true if the entry was changed, false otherwise.
     */
    public boolean abbreviate(BibDatabase database, BibEntry entry, Field fieldName, CompoundEdit compoundEdit) {
        if (!entry.hasField(fieldName)) {
            return false;
        }

        String origText = entry.getField(fieldName).get();
        String text = database != null ? database.resolveForStrings(origText) : origText;

        Optional<Abbreviation> foundAbbreviation = journalAbbreviationRepository.get(text);

        if (foundAbbreviation.isEmpty() && abbreviationType != AbbreviationType.LTWA) {
            return false; // Unknown, cannot abbreviate anything.
        }

        Optional<String> newTextOptional = abbreviationType == AbbreviationType.LTWA
                                           ? journalAbbreviationRepository.getLtwaAbbreviation(text)
                                           : foundAbbreviation.map(this::getAbbreviatedName);

        // Return early if no abbreviation found or it matches original
        if (newTextOptional.isEmpty() || newTextOptional.get().equals(origText)) {
            return false;
        }

        String newText = newTextOptional.get();

        // Store full name into fjournal but only if it exists
        foundAbbreviation.ifPresent(abbr -> {
            if (useFJournalField && (StandardField.JOURNAL == fieldName || StandardField.JOURNALTITLE == fieldName)) {
                String fullName = abbr.getName();
                entry.setField(AMSField.FJOURNAL, fullName);
                compoundEdit.addEdit(new UndoableFieldChange(entry, AMSField.FJOURNAL, null, fullName));
            }
        });

        entry.setField(fieldName, newText);
        compoundEdit.addEdit(new UndoableFieldChange(entry, fieldName, origText, newText));
        return true;
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
