package org.jabref.gui.autocompleter;

import java.util.Set;

import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class AutoCompletePreferences {

    public enum NameFormat {
        LAST_FIRST, FIRST_LAST, BOTH
    }

    private final boolean shouldAutoComplete;
    private final AutoCompleteFirstNameMode firstNameMode;
    private final NameFormat nameFormat;
    private final Set<Field> completeFields;
    private final JournalAbbreviationPreferences journalAbbreviationPreferences;

    public AutoCompletePreferences(boolean shouldAutoComplete, AutoCompleteFirstNameMode firstNameMode, NameFormat nameFormat, Set<Field> completeFields, JournalAbbreviationPreferences journalAbbreviationPreferences) {
        this.shouldAutoComplete = shouldAutoComplete;
        this.firstNameMode = firstNameMode;
        this.nameFormat = nameFormat;
        this.completeFields = completeFields;
        this.journalAbbreviationPreferences = journalAbbreviationPreferences;
    }

    public boolean shouldAutoComplete() {
        return shouldAutoComplete;
    }

    /**
     * Returns how the first names are handled.
     */
    public AutoCompleteFirstNameMode getFirstNameMode() {
        return firstNameMode;
    }

    public NameFormat getNameFormat() {
        return nameFormat;
    }

    /**
     * Returns the list of fields for which autocomplete is enabled
     *
     * @return List of field names
     */
    public Set<Field> getCompleteFields() {
        return completeFields;
    }

    public String getCompleteNamesAsString() {
        return FieldFactory.serializeFieldsList(completeFields);
    }

    public JournalAbbreviationPreferences getJournalAbbreviationPreferences() {
        return journalAbbreviationPreferences;
    }
}
