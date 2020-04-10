package org.jabref.gui.autocompleter;

import java.util.Set;

import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class AutoCompletePreferences {

    public enum onlyCompleteNameFormat {
        LAST_FIRST, FIRST_LAST, BOTH
    }

    private boolean shouldAutoComplete;
    private AutoCompleteFirstNameMode firstNameMode;
    private AutoCompletePreferences.onlyCompleteNameFormat onlyCompleteNameFormat;
    private Set<Field> completeFields;
    private final JournalAbbreviationPreferences journalAbbreviationPreferences;

    public AutoCompletePreferences(boolean shouldAutoComplete, AutoCompleteFirstNameMode firstNameMode, AutoCompletePreferences.onlyCompleteNameFormat onlyCompleteNameFormat, Set<Field> completeFields, JournalAbbreviationPreferences journalAbbreviationPreferences) {
        this.shouldAutoComplete = shouldAutoComplete;
        this.firstNameMode = firstNameMode;
        this.onlyCompleteNameFormat = onlyCompleteNameFormat;
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

    public AutoCompletePreferences.onlyCompleteNameFormat getOnlyCompleteNameFormat() { return onlyCompleteNameFormat; }

    /**
     * Returns the list of fields for which autocomplete is enabled
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
